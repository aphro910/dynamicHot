package com.kuma.tools.dynamicHot.context;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HotKeyContext {
    public static volatile int flag = 0;
    public static Map<String, Integer> keyMap = new ConcurrentHashMap<>();

    public static Set<String> hotKey = new HashSet<>();
}
