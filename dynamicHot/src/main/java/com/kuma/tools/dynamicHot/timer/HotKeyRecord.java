package com.kuma.tools.dynamicHot.timer;

import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.record.HotRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HotKeyRecord {

    @Autowired
    private HotRecord hotRecord;
    @Autowired
    private HotKeyContext hotKeyContext;

    @Scheduled(initialDelayString = "${spring.dynamic.hotkey.record.initial-delay:5000}",
            fixedRateString = "${spring.dynamic.hotkey.record.fixed-rate:5000}")
    public void record() {
        if (!hotKeyContext.keyMap.isEmpty()) {
            hotKeyContext.lock.writeLock().lock();
            hotRecord.record(hotKeyContext.keyMap);
            hotKeyContext.keyMap.clear();
            hotKeyContext.timestamp = System.currentTimeMillis();
            hotKeyContext.lock.writeLock().unlock();
        }
    }
}
