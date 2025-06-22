package com.kuma.tools.dynamicHot.context;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class HotKeyContext {
    public static Map<String, Integer> hotKeyMap = new ConcurrentHashMap<>();
}
