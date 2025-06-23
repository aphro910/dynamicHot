package com.kuma.tools.dynamicHot.config;

import com.kuma.tools.dynamicHot.aspect.HotKeyDetect;
import com.kuma.tools.dynamicHot.mqnotify.RocketMQNotify;
import com.kuma.tools.dynamicHot.schedule.HotKeyReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "rocketmq")
public class HotKeyConditionalConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public RocketMQNotify RocketMQNotify() {
        return new RocketMQNotify();
    }
}