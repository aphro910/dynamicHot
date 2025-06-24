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

    @Scheduled(initialDelay = 5000, fixedRate = 5000)
    public void report() {
        HotKeyContext.flag = 1;
        if (!HotKeyContext.keyMap.isEmpty()) {
            mqNotify.report(HotKeyContext.keyMap);
            HotKeyContext.keyMap = new ConcurrentHashMap<>();
        }
        HotKeyContext.flag = 0;
    }
}
