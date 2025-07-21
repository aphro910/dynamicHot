package com.kuma.tools.dynamicHotCompute.handler;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.kuma.tools.dynamicHotCompute.consts.Constants;
import com.kuma.tools.dynamicHotCompute.context.ChannelContext;
import com.kuma.tools.dynamicHotCompute.elasticsearch.entity.HotKeyEntity;
import com.kuma.tools.dynamicHotCompute.mapper.HotKeyMapper;
import com.kuma.tools.dynamicHotCompute.protobuf.DataModel;
import com.kuma.tools.dynamicHotCompute.utils.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Component
public class HotKeyHandler {

    private static final Logger log = LoggerFactory.getLogger(HotKeyHandler.class);
    private Map<String, Deque<DataModel.Message>> keyMap;
    private List<HotKeyEntity> hotKeyList;

    @Autowired
    @Qualifier("hotKeyComputeThreadPool")
    private ExecutorService hotKeyComputeThreadPool;
    @Autowired
    @Qualifier("singleScheduledThreadPool")
    private ScheduledExecutorService singleScheduledThreadPool;
    @Autowired
    @Qualifier("hotkeySaveThreadPool")
    private ScheduledExecutorService hotkeySaveThreadPool;
    @Autowired
    private HotKeyMapper hotKeyMapper;

    @Value("${spring.dynamic.hotkey.compute.maxKeySize:-1}")
    private Integer maxKeySize;
    @Value("${spring.dynamic.hotkey.compute.timeRange:5000}")
    private long timeRange;
    @Value("${spring.dynamic.hotkey.compute.hotCount:5000}")
    private int hotCount;

    private static final int CHUNK_SIZE = 1000; // 每块1000个键
    private List<List<Deque<DataModel.Message>>> dequeList;
    private static final int concurrency = 16; //此处需为2^n,否则下面的取模运算会出问题


    @PostConstruct
    public void init() {
        if (maxKeySize > 0) {
            //设置最大key size,超过则通过LRU进行淘汰
            ConcurrentLinkedHashMap.Builder<String, Deque<DataModel.Message>> builder = new ConcurrentLinkedHashMap.Builder<>();
            keyMap = builder.maximumWeightedCapacity(maxKeySize).build();
        } else {
            //不设置最大key size,只保证数据写入的原子性,可能有OOM风险
            keyMap = new ConcurrentHashMap<>();
        }
        hotKeyList = new ArrayList<>();
        dequeList = new ArrayList<>(concurrency);
        for (int i = 0; i < concurrency; i++) {
            dequeList.add(new ArrayList<>());
        }

        singleScheduledThreadPool.scheduleWithFixedDelay(this::compute, 0, 10, TimeUnit.MILLISECONDS);
        hotkeySaveThreadPool.scheduleWithFixedDelay(this::saveHotKeyHistory, 500, timeRange, TimeUnit.MILLISECONDS);
    }

    public boolean hasHotKey() {
        return hotKeyList.isEmpty();
    }

    public void add(DataModel.Message message) {
        String key = message.getKey();
        keyMap.computeIfAbsent(key, k -> new ArrayDeque<>()).addFirst(message);
        //移除过期的数据
        if (keyMap.get(key).getLast().getTimestamp() < (System.currentTimeMillis() - timeRange)) {
            keyMap.get(key).removeLast();
            if (keyMap.get(key).isEmpty()) {
                keyMap.remove(key);
            }
        }
    }

    public void compute() {
        if (keyMap.isEmpty()) {
            try {
                Thread.sleep(100);
                return;
            } catch (InterruptedException e) {
                log.error(e.getMessage());
            }
        }
        List<HotKeyEntity> hotKeyList = new ArrayList<>();
        List<Future<List<HotKeyEntity>>> futures = new ArrayList<>();

        for (Map.Entry<String, Deque<DataModel.Message>> entry : keyMap.entrySet()) {
            String key = entry.getKey();
            int bucket = Math.abs(key.hashCode()) & (concurrency-1);
            dequeList.get(bucket).add(entry.getValue());
        }

        for (List<Deque<DataModel.Message>> item : dequeList) {
            try {
                Future<List<HotKeyEntity>> future = hotKeyComputeThreadPool.submit(new Callable<List<HotKeyEntity>>() {
                    @Override
                    public List<HotKeyEntity> call() throws Exception {
                        return executeBatch(item);
                    }
                });
                futures.add(future);
            } catch (RejectedExecutionException e) {
                log.warn(e.getMessage());
            }
        }

        for (Future<List<HotKeyEntity>> future : futures) {
            try {
                List<HotKeyEntity> hotkeys = future.get();
                hotKeyList.addAll(hotkeys);
            } catch (ExecutionException | InterruptedException e) {
                return;
            }
        }
        this.hotKeyList = hotKeyList;
        //dequeList重复利用,减少内存消耗和GC次数
        dequeClear();
    }

    private void dequeClear() {
        for (int i = 0; i < concurrency; i++) {
            dequeList.get(i).clear();
        }
    }

    public void pushToClient() {
        if (!hotKeyList.isEmpty()) {
            sendHotKeysInChunks(hotKeyList);
        }
    }

    public void saveHotKeyHistory() {
        if (!hotKeyList.isEmpty()) {
            saveToES(hotKeyList);
        }
    }

    private List<HotKeyEntity> executeBatch(List<Deque<DataModel.Message>> batch) {
        long nowTime = System.currentTimeMillis();
        List<HotKeyEntity> hotKeyList = new ArrayList<>();
        for (Deque<DataModel.Message> messages : batch) {
            if (messages.isEmpty()) {
                break;
            }
            int count = 0;
            for (DataModel.Message message : messages) {
                if (nowTime - timeRange <= message.getTimestamp()) {
                    count += message.getCount();
                } else {
                    break;
                }
            }
            if (count >= hotCount) {
                HotKeyEntity hotKeyEntity = new HotKeyEntity();
                hotKeyEntity.setKey(messages.getFirst().getKey());
                hotKeyEntity.setTimestamp(nowTime);
                hotKeyEntity.setCount(count);
                hotKeyList.add(hotKeyEntity);
            }
        }
        return hotKeyList;
    }

    // 分块发送热键列表
    private void sendHotKeysInChunks(List<HotKeyEntity> hotKeyEntityList) {
        List<String> hotKeyList = hotKeyEntityList.stream().map(HotKeyEntity::getKey).collect(Collectors.toList());
        String sessionId = UUID.randomUUID().toString();
        int totalChunks = (int) Math.ceil((double) hotKeyList.size() / CHUNK_SIZE);
        ByteBufAllocator allocator = ChannelContext.channelGroup.iterator().next().alloc();
        // 发送开始标记
        DataModel.HotKeyChunkInfo chunkStart = DataModel.HotKeyChunkInfo.newBuilder()
                .setType(Constants.CHUNK_START)
                .setSessionId(sessionId)
                .setChunkSize(totalChunks)
                .build();

        try {
            byte[] compressed = CompressUtil.compress(chunkStart.toByteArray());

            ByteBuf buffer = allocator.buffer(compressed.length);
            buffer.writeBytes(compressed);
            ChannelContext.channelGroup.writeAndFlush(buffer);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 分块发送数据
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, hotKeyList.size());
            List<String> chunkList = hotKeyList.subList(start, end);

            // 构建分块数据
            DataModel.HotKeyChunkInfo chunkData = DataModel.HotKeyChunkInfo.newBuilder()
                    .setType(Constants.CHUNK_DATA)
                    .setSessionId(sessionId)
                    .setChunkIndex(i)
                    .setChunkSize(totalChunks)
                    .addAllData(chunkList)
                    .build();

            try {
                // 压缩并发送
                byte[] compressed = CompressUtil.compress(chunkData.toByteArray());

                ByteBuf buffer = allocator.buffer(compressed.length);
                buffer.writeBytes(compressed);
                ChannelContext.channelGroup.writeAndFlush(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 发送结束标记
        DataModel.HotKeyChunkInfo chunkEnd = DataModel.HotKeyChunkInfo.newBuilder()
                .setType(Constants.CHUNK_END)
                .setSessionId(sessionId)
                .setChunkSize(totalChunks)
                .build();
        try {
            // 压缩并发送
            byte[] compressed = CompressUtil.compress(chunkEnd.toByteArray());
            ByteBuf buffer = allocator.buffer(compressed.length);
            buffer.writeBytes(compressed);
            ChannelContext.channelGroup.writeAndFlush(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void saveToES(List<HotKeyEntity> hotKeyList) {
        int totalChunks = (int) Math.ceil((double) hotKeyList.size() / CHUNK_SIZE);
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, hotKeyList.size());
            List<HotKeyEntity> subList = hotKeyList.subList(start, end);
            hotKeyMapper.saveAll(subList);
        }
    }

    @PreDestroy
    public void stop() {
        singleScheduledThreadPool.shutdown();
        hotKeyComputeThreadPool.shutdown();
        hotkeySaveThreadPool.shutdown();
    }
}
