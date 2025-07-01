package com.kuma.tools.dynamicHotCompute.timer;

import com.kuma.tools.dynamicHotCompute.handler.HotKeyHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class HotKeyCompute {

    @Autowired
    private HotKeyHandler hotKeyHandler;

    @Scheduled(initialDelayString = "${spring.dynamic.hotkey.compute.initial-delay:500}",
            fixedRateString = "${spring.dynamic.hotkey.compute.fixed-rate:500}")
    public void compute() {
        if (!hotKeyHandler.isEmpty()) {
            hotKeyHandler.compute();
        }
    }
}
