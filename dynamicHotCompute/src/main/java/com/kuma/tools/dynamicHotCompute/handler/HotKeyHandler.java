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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

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
        for (Map.Entry<String, Deque<Message>> entry : keyMap.entrySet()) {
            hotKeyComputeExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    Deque<Message> messages = entry.getValue();
                    int count = 0;
                    for (Message message : messages) {
                        if (System.currentTimeMillis() - timeRange <= message.getTimestamp()) {
                            count += message.getCount();
                            if (count >= hotCount) {
                                hotKeyList.add(message.getKey());
                                break;
                            }
                        }

                    }
                }
            });
        }
        if (!hotKeyList.isEmpty()) {
            for (Channel channel : ChannelContext.channels) {
                try {
                    byte[] binaryData = CompressUtil.compress(JSONUtil.toJsonStr(hotKeyList));
                    ByteBuf buffer = Unpooled.wrappedBuffer(binaryData);
                    channel.writeAndFlush(new BinaryWebSocketFrame(buffer));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
