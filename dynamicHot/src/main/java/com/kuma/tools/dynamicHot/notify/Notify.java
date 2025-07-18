package com.kuma.tools.dynamicHot.notify;

import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.notify.netty.NettyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class Notify {

    @Autowired
    NettyClient nettyClient;
    @Autowired
    private HotKeyContext hotKeyContext;
    @Autowired
    @Qualifier("singleScheduledThreadPool")
    private ScheduledExecutorService singleScheduledThreadPool;

    @Value("${spring.dynamic.hotkey.collect.initial-delay:100}")
    private long initialDelay;
    @Value("${spring.dynamic.hotkey.compute.fixed-rate:500}")
    private long fixedRate;

    @PostConstruct
    public void send() {
        singleScheduledThreadPool.scheduleWithFixedDelay(
                this::push,
                initialDelay,
                fixedRate,
                TimeUnit.MILLISECONDS);
    }

    private void push() {
        if (!hotKeyContext.keyMap.isEmpty()) {
            hotKeyContext.lock.writeLock().lock();
            nettyClient.send(hotKeyContext.keyMap);
            hotKeyContext.keyMap.clear();
            hotKeyContext.lock.writeLock().unlock();
        }
    }

    @PreDestroy
    public void stop() {
        singleScheduledThreadPool.shutdown();
    }
}
