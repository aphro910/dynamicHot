package com.kuma.tools.dynamicHot.schedule;

import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.mqnotify.MQNotify;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class HotKeyReport {

    @Autowired
    private MQNotify mqNotify;

    @Scheduled(initialDelay = 5000, fixedRate = 5000)
    public void report() {
        HotKeyContext.flag = 1;
        mqNotify.report(HotKeyContext.hotKeyMap);
        HotKeyContext.hotKeyMap = new ConcurrentHashMap<>();
        HotKeyContext.flag = 0;
    }
}
