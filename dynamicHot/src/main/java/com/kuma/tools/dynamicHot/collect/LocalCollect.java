package com.kuma.tools.dynamicHot.collect;

import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.record.HotRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Map;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.record.type", havingValue = "local", matchIfMissing = true)
public class LocalCollect implements HotCollect {

    @Autowired
    HotKeyContext hotKeyContext;

    @Override
    public void collect() {
        hotKeyContext.hotKey = new HashSet<>(hotKeyContext.keyMap.keySet());
    }
}
