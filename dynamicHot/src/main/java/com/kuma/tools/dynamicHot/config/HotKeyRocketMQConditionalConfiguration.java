package com.kuma.tools.dynamicHot.config;

import com.kuma.tools.dynamicHot.consumer.RocketMQConsumer;
import com.kuma.tools.dynamicHot.notify.RocketMQNotify;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "rocketmq")
public class HotKeyRocketMQConditionalConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RocketMQNotify RocketMQNotify() {
        return new RocketMQNotify();
    }

    @Bean
    @ConditionalOnMissingBean
    public RocketMQConsumer RocketMQConsumer() {
        return new RocketMQConsumer();
    }
}