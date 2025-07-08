package com.kuma.tools.dynamicHotCompute.handler;

import cn.hutool.json.JSONUtil;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.kuma.tools.dynamicHotCompute.consts.Constants;
import com.kuma.tools.dynamicHotCompute.context.ChannelContext;
import com.kuma.tools.dynamicHotCompute.entity.Chunk;
import com.kuma.tools.dynamicHotCompute.entity.ChunkInfo;
import com.kuma.tools.dynamicHotCompute.entity.Message;
import com.kuma.tools.dynamicHotCompute.utils.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
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

    private Map<String, Deque<Message>> keyMap;
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


    @PostConstruct
    public void initMap() {
        if (maxKeySize > 0) {
            //设置最大key size,超过则通过LRU进行淘汰
            ConcurrentLinkedHashMap.Builder<String, Deque<Message>> builder = new ConcurrentLinkedHashMap.Builder<>();
            keyMap = builder.maximumWeightedCapacity(maxKeySize).build();
        } else {
            //不设置最大key size,只保证数据写入的原子性,可能有OOM风险
            keyMap = new ConcurrentHashMap<>();
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
        return keyMap.isEmpty();
    }

    public void add(Message message) {
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
                e.printStackTrace();
            }
        }
        List<String> hotKeyList = new ArrayList<>();
        List<Future<List<String>>> futures = new ArrayList<>();
        List<List<Deque<Message>>> dequeList = new ArrayList<>(10);
        for (int i = 0; i < 10; i++) {
            dequeList.add(new ArrayList<>());
        }

        for (Map.Entry<String, Deque<Message>> entry : keyMap.entrySet()) {
            String key = entry.getKey();
            int bucket = Math.abs(key.hashCode()) % 10;
            dequeList.get(bucket).add(entry.getValue());
        }

        for (List<Deque<Message>> item : dequeList) {
            Future<List<String>> future = hotKeyComputeExecutor.submit(new Callable<List<String>>() {
                @Override
                public List<String> call() throws Exception {
                    return executeBatch(item);
                }
            });
            futures.add(future);
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
    }

    public void push() {
        if (!hotKeyList.isEmpty()) {
            sendHotKeysInChunks(hotKeyList);
        }
    }

    private List<String> executeBatch(List<Deque<Message>> batch) {
        long nowTime = System.currentTimeMillis();
        List<String> hotKeyList = new ArrayList<>();
        for (Deque<Message> messages : batch) {
            if (messages.isEmpty()) {
                break;
            }
            int count = 0;
            for (Message message : messages) {
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
        ChunkInfo chunkStart = new ChunkInfo();
        chunkStart.setType(Constants.CHUNK_START);
        chunkStart.setSessionId(sessionId);
        chunkStart.setChunkSize(totalChunks);
        ChannelContext.channelGroup.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(chunkStart)));

        // 分块发送数据
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, hotKeyList.size());
            List<String> chunkList = hotKeyList.subList(start, end);

            // 构建分块数据
            Chunk chunkData = new Chunk();
            chunkData.setSessionId(sessionId);
            chunkData.setIndex(i);
            chunkData.setTotal(totalChunks);
            chunkData.setData(chunkList);

            try {
                // 压缩并发送
                byte[] compressed = CompressUtil.compress(JSONUtil.toJsonStr(chunkData));
                ByteBuf buffer = Unpooled.wrappedBuffer(compressed);
                ChannelContext.channelGroup.writeAndFlush(new BinaryWebSocketFrame(buffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        // 发送结束标记
        ChunkInfo chunkEnd = new ChunkInfo();
        chunkEnd.setType(Constants.CHUNK_END);
        chunkEnd.setSessionId(sessionId);
        ChannelContext.channelGroup.writeAndFlush(new TextWebSocketFrame(JSONUtil.toJsonStr(chunkEnd)));
    }

    @PreDestroy
    public void stop() {
        isStop = true;
        singleExecutor.shutdown();
        hotKeyComputeExecutor.shutdown();

    }
}
