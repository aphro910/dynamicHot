package com.kuma.tools.dynamicHot.context;

import com.googlecode.concurrentlinkedhashmap.ConcurrentLinkedHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Component
public class HotKeyContext {
    public ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public Map<String, Integer> keyMap;
    public Map<String, Set<String>> hotKey = new ConcurrentHashMap<>();

    @Value("${spring.dynamic.hotkey.report.maxSize:-1}")
    private Integer maxSize;

    @PostConstruct
    public void initMap() {
        if (maxSize > 0) {
            ConcurrentLinkedHashMap.Builder<String, Integer> builder = new ConcurrentLinkedHashMap.Builder<>();
            keyMap = builder.maximumWeightedCapacity(maxSize).build();
        } else {
            keyMap = new ConcurrentHashMap<>();
        }
    }
}
