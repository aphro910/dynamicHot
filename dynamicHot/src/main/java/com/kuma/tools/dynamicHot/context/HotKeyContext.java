package com.kuma.tools.dynamicHot.context;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class HotKeyContext {
    public static Map<String, Set<Long>> hotKeyMap = new ConcurrentHashMap<>();
}
