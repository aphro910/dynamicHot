package com.kuma.tools.dynamicHotCompute.handler;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.kuma.tools.dynamicHotCompute.consts.Constants;
import com.kuma.tools.dynamicHotCompute.context.ChannelContext;
import com.kuma.tools.dynamicHotCompute.protobuf.DataModel;
import com.kuma.tools.dynamicHotCompute.utils.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
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

@Component
public class HotKeyHandler {

    private static final Logger log = LoggerFactory.getLogger(HotKeyHandler.class);
    private Map<String, Deque<DataModel.Message>> keyMap;
    private List<String> hotKeyList;

    @Autowired
    @Qualifier("hotKeyComputeThreadPool")
    private ExecutorService hotKeyComputeExecutor;
    @Autowired
    @Qualifier("singleThreadPool")
    private ExecutorService singleExecutor;

    @Value("${spring.dynamic.hotkey.compute.maxKeySize:-1}")
    private Integer maxKeySize;
    @Value("${spring.dynamic.hotkey.compute.timeRange:5000}")
    private long timeRange;
    @Value("${spring.dynamic.hotkey.compute.hotCount:5000}")
    private int hotCount;

    private boolean isStop = false;
    private static final int CHUNK_SIZE = 500; // 每块500个键
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

        singleExecutor.execute(new Runnable() {
            @Override
            public void run() {
                while (!isStop) {
                    compute();
                }
            }
        });
    }

    public boolean isEmpty() {
        return hotKeyList.isEmpty();
    }

    public void add(DataModel.Message message) {
        String key = message.getKey();
        keyMap.computeIfAbsent(key, k -> new ArrayDeque<>()).addFirst(message);
        //移除过期的数据
        if (keyMap.get(key).getLast().getTimestamp() < System.currentTimeMillis() - timeRange) {
            keyMap.get(key).removeLast();
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
        List<String> hotKeyList = new ArrayList<>();
        List<Future<List<String>>> futures = new ArrayList<>();

        for (Map.Entry<String, Deque<DataModel.Message>> entry : keyMap.entrySet()) {
            String key = entry.getKey();
            int bucket = Math.abs(key.hashCode()) & (concurrency-1);
            dequeList.get(bucket).add(entry.getValue());
        }

        for (List<Deque<DataModel.Message>> item : dequeList) {
            try {
                Future<List<String>> future = hotKeyComputeExecutor.submit(new Callable<List<String>>() {
                    @Override
                    public List<String> call() throws Exception {
                        return executeBatch(item);
                    }
                });
                futures.add(future);
            } catch (RejectedExecutionException e) {
                log.warn(e.getMessage());
            }
        }

        for (Future<List<String>> future : futures) {
            try {
                List<String> hotkeys = future.get();
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

    public void push() {
        if (!hotKeyList.isEmpty()) {
            sendHotKeysInChunks(hotKeyList);
        }
    }

    private List<String> executeBatch(List<Deque<DataModel.Message>> batch) {
        long nowTime = System.currentTimeMillis();
        List<String> hotKeyList = new ArrayList<>();
        for (Deque<DataModel.Message> messages : batch) {
            if (messages.isEmpty()) {
                break;
            }
            int count = 0;
            for (DataModel.Message message : messages) {
                if (nowTime - timeRange <= message.getTimestamp()) {
                    count += message.getCount();
                    if (count >= hotCount) {
                        hotKeyList.add(message.getKey());
                        break;
                    }
                } else {
                    break;
                }
            }
        }
        return hotKeyList;
    }

    // 分块发送热键列表
    private void sendHotKeysInChunks(List<String> hotKeyList) {
        String sessionId = UUID.randomUUID().toString();
        int totalChunks = (int) Math.ceil((double) hotKeyList.size() / CHUNK_SIZE);
        // 发送开始标记
        DataModel.ChunkInfo chunkStart = DataModel.ChunkInfo.newBuilder()
                .setType(Constants.CHUNK_START)
                .setSessionId(sessionId)
                .setChunkSize(totalChunks)
                .build();

        try {
            byte[] compressed = CompressUtil.compress(chunkStart.toByteArray());
            ByteBuf buffer = Unpooled.wrappedBuffer(compressed);
            ChannelContext.channelGroup.writeAndFlush(new BinaryWebSocketFrame(buffer));
        } catch (Exception e) {
            e.printStackTrace();
        }

        // 分块发送数据
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, hotKeyList.size());
            List<String> chunkList = hotKeyList.subList(start, end);

            // 构建分块数据
            DataModel.ChunkInfo chunkData = DataModel.ChunkInfo.newBuilder()
                    .setType(Constants.CHUNK_DATA)
                    .setSessionId(sessionId)
                    .setChunkIndex(i)
                    .setChunkSize(totalChunks)
                    .addAllData(chunkList)
                    .build();

            try {
                // 压缩并发送
                byte[] compressed = CompressUtil.compress(chunkData.toByteArray());
                ByteBuf buffer = Unpooled.wrappedBuffer(compressed);
                ChannelContext.channelGroup.writeAndFlush(new BinaryWebSocketFrame(buffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 发送结束标记
        DataModel.ChunkInfo chunkEnd = DataModel.ChunkInfo.newBuilder()
                .setType(Constants.CHUNK_END)
                .setSessionId(sessionId)
                .setChunkSize(totalChunks)
                .build();
        try {
            // 压缩并发送
            byte[] compressed = CompressUtil.compress(chunkEnd.toByteArray());
            ByteBuf buffer = Unpooled.wrappedBuffer(compressed);
            ChannelContext.channelGroup.writeAndFlush(new BinaryWebSocketFrame(buffer));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stop() {
        isStop = true;
        singleExecutor.shutdown();
        hotKeyComputeExecutor.shutdown();

    }
}
