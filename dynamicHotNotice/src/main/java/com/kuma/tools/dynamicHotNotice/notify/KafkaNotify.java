package com.kuma.tools.dynamicHotNotice.notify;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "kafka")
public class KafkaNotify implements MQNotify {


    @Override
    public void notice(String msg) {
        System.out.println("kafka send notice");
    }
}
