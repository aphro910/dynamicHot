package com.kuma.tools.dynamicHot.timer;

import com.kuma.tools.dynamicHot.collect.HotCollect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HotKeyCollect {

    @Autowired
    HotCollect hotCollect;

    @Scheduled(initialDelayString = "${spring.dynamic.hotkey.collect.initial-delay:5000}",
            fixedRateString = "${spring.dynamic.hotkey.collect.fixed-rate:5000}")
    public void collect() {
        hotCollect.collect();
    }
}
