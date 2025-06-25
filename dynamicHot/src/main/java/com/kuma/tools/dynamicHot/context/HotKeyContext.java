package com.kuma.tools.dynamicHot.context;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class HotKeyContext {
    public static ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    public static long timestamp = System.currentTimeMillis();
    public static Map<String, Integer> keyMap = new ConcurrentHashMap<>();
    public static Set<String> hotKey = new HashSet<>();
}
