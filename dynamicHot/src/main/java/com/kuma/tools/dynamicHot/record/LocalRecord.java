package com.kuma.tools.dynamicHot.record;

import com.kuma.tools.dynamicHot.context.HotKeyContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.record.type", havingValue = "local", matchIfMissing = true)
public class LocalRecord implements HotRecord {

    @Autowired
    HotKeyContext hotKeyContext;

    @Override
    public void record(Map<String, Integer> map) {
    }
}
