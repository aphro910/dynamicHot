package com.kuma.tools.dynamicHot.mqnotify;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "kafka")
public class KafkaNotify implements MQNotify{

//    @Autowired
//    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void report(Map<String, Integer> map) {
        System.out.println("kafka send notify");
    }
}
