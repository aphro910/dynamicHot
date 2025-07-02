package com.kuma.tools.dynamicHotCompute.handler;

import cn.hutool.json.JSONUtil;
import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import com.kuma.tools.dynamicHotCompute.context.ChannelContext;
import com.kuma.tools.dynamicHotCompute.entity.Message;
import com.kuma.tools.dynamicHotCompute.utils.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

@Component
public class HotKeyHandler {

    private Map<String, Deque<Message>> keyMap;

    @Autowired
    @Qualifier("hotKeyComputeThreadPool")
    private ThreadPoolTaskExecutor hotKeyComputeExecutor;

    @Value("${spring.dynamic.hotkey.compute.maxKeySize:-1}")
    private Integer maxKeySize;
    @Value("${spring.dynamic.hotkey.compute.timeRange:5000}")
    private long timeRange;
    @Value("${spring.dynamic.hotkey.compute.hotCount:5000}")
    private int hotCount;

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
                e.printStackTrace();
            }
        }

        if (!hotKeyList.isEmpty()) {
            try {
                byte[] binaryData = CompressUtil.compress(JSONUtil.toJsonStr(hotKeyList));
                ByteBuf buffer = Unpooled.wrappedBuffer(binaryData);
                ChannelContext.channelGroup.writeAndFlush(new BinaryWebSocketFrame(buffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private List<String> executeBatch(List<Deque<Message>> batch) {
        List<String> hotKeyList = new ArrayList<>();
        for (Deque<Message> messages : batch) {
            if (System.currentTimeMillis() - timeRange > messages.peekFirst().getTimestamp()) {
                break;
            }
            int count = 0;
            for (Message message : messages) {
                if (System.currentTimeMillis() - timeRange <= message.getTimestamp()) {
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
}
