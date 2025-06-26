package com.kuma.tools.dynamicHot.consumer;


import lombok.extern.log4j.Log4j2;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@Log4j2
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "kafka")
public class KafkaMQConsumer implements HotKeyMQConsumer {

    @Override
    public void start() {
        System.out.println("kafka notice");
    }

}
