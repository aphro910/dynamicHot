package com.kuma.tools.dynamicHot.config;

import com.kuma.tools.dynamicHot.aspect.HotKeyDetect;
import com.kuma.tools.dynamicHot.cache.AllCache;
import com.kuma.tools.dynamicHot.cache.CaffeineLocalCache;
import com.kuma.tools.dynamicHot.cache.RedisCache;
import com.kuma.tools.dynamicHot.consumer.RocketMQConsumer;
import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.notify.LocalNotify;
import com.kuma.tools.dynamicHot.notify.RocketMQNotify;
import com.kuma.tools.dynamicHot.timer.HotKeyReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HotKeyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HotKeyDetect hotKeyDetect() {
        return new HotKeyDetect();
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeyReport HotKeyReport() {
        return new HotKeyReport();
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeyContext HotKeyContext() {
        return new HotKeyContext();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.cache.type", havingValue = "local", matchIfMissing = true)
    public CaffeineLocalCache CaffeineLocalCache() {
        return new CaffeineLocalCache();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "local", matchIfMissing = true)
    public LocalNotify LocalNotify() {
        return new LocalNotify();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.cache.type", havingValue = "redis")
    public RedisCache RedisCache() {
        return new RedisCache();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.cache.type", havingValue = "all")
    public AllCache AllCache() {
        return new AllCache();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "rocketmq")
    public RocketMQNotify RocketMQNotify() {
        return new RocketMQNotify();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "rocketmq")
    public RocketMQConsumer RocketMQConsumer() {
        return new RocketMQConsumer();
    }

}