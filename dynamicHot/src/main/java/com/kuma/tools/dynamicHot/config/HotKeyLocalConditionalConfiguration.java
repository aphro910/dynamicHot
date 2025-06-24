package com.kuma.tools.dynamicHot.config;

import com.kuma.tools.dynamicHot.consumer.RocketMQConsumer;
import com.kuma.tools.dynamicHot.notify.LocalNotify;
import com.kuma.tools.dynamicHot.notify.RocketMQNotify;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "local")
public class HotKeyLocalConditionalConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public LocalNotify LocalNotify() {
        return new LocalNotify();
    }
}