package com.kuma.tools.dynamicHot.timer;

import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.notify.Notify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HotKeyReport {

    @Autowired
    private Notify notify;
    @Autowired
    private HotKeyContext hotKeyContext;

    @Scheduled(initialDelayString = "${spring.dynamic.hotkey.collect.initial-delay:100}",
            fixedRateString = "${spring.dynamic.hotkey.collect.fixed-rate:500}")
    public void report() {
        if (!hotKeyContext.keyMap.isEmpty()) {
            hotKeyContext.lock.writeLock().lock();
            notify.send(hotKeyContext.keyMap);
            hotKeyContext.keyMap.clear();
            hotKeyContext.lock.writeLock().unlock();
        }
    }
}
