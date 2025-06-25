package com.kuma.tools.dynamicHot.timer;

import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.notify.MQNotify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class HotKeyReport {

    @Autowired
    private MQNotify mqNotify;

    @Scheduled(initialDelayString = "${spring.dynamic.hotkey.collect.initial-delay:5000}",
            fixedRateString = "${spring.dynamic.hotkey.collect.fixed-rate:5000}")
    public void report() {
        if (!HotKeyContext.keyMap.isEmpty()) {
            mqNotify.report(HotKeyContext.keyMap);
            HotKeyContext.keyMap.clear();
        }
    }
}
