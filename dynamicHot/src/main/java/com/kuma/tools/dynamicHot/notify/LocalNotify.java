package com.kuma.tools.dynamicHot.notify;

import com.kuma.tools.dynamicHot.context.HotKeyContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "local")
public class LocalNotify implements MQNotify{

    @Autowired
    HotKeyContext hotKeyContext;

    @Override
    public void report(Map<String, Integer> map) {
        hotKeyContext.hotKey = new HashSet<>(map.keySet());
    }
}
