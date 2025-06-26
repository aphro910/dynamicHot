package com.kuma.tools.dynamicHot.config;

import com.kuma.tools.dynamicHot.aspect.HotKeyDetect;
import com.kuma.tools.dynamicHot.cache.AllCache;
import com.kuma.tools.dynamicHot.cache.CaffeineLocalCache;
import com.kuma.tools.dynamicHot.cache.RedisCache;
import com.kuma.tools.dynamicHot.collect.ElasticCollect;
import com.kuma.tools.dynamicHot.collect.LocalCollect;
import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.record.ElasticRecord;
import com.kuma.tools.dynamicHot.record.LocalRecord;

import com.kuma.tools.dynamicHot.timer.HotKeyCollect;
import com.kuma.tools.dynamicHot.timer.HotKeyRecord;
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
    public HotKeyRecord HotKeyRecord() {
        return new HotKeyRecord();
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeyCollect HotKeyCollect() {
        return new HotKeyCollect();
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
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.record.type", havingValue = "local", matchIfMissing = true)
    public LocalRecord LocalNotify() {
        return new LocalRecord();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.record.type", havingValue = "elastic")
    public ElasticRecord ElasticRecord() {
        return new ElasticRecord();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.record.type", havingValue = "local", matchIfMissing = true)
    public LocalCollect LocalCollect() {
        return new LocalCollect();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.record.type", havingValue = "elastic")
    public ElasticCollect ElasticCollect() {
        return new ElasticCollect();
    }


}