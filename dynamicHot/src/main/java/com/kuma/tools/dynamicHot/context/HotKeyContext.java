package com.kuma.tools.dynamicHot.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HotKeyContext {
    public static volatile int flag = 0;
    public static Map<String, Integer> hotKeyMap = new ConcurrentHashMap<>();
}
