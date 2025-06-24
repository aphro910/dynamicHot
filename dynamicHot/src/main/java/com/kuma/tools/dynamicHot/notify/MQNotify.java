package com.kuma.tools.dynamicHot.notify;

import java.util.Map;

public interface MQNotify {

    void report(Map<String, Integer> map);
}
