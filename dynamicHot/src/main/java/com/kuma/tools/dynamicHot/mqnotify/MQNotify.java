package com.kuma.tools.dynamicHot.mqnotify;

import java.util.Map;

public interface MQNotify {

    void report(Map<String, Integer> map);
}
