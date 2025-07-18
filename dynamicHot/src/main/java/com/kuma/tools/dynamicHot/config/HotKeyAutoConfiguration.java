package com.kuma.tools.dynamicHot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.kuma.tools.dynamicHot.aspect.HotKeyDetect;
import com.kuma.tools.dynamicHot.cache.CaffeineLocalCache;
import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.notify.Notify;
import com.kuma.tools.dynamicHot.notify.netty.ClientHandler;
import com.kuma.tools.dynamicHot.notify.netty.NettyClient;
import com.kuma.tools.dynamicHot.notify.register.NacosRegister;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.CacheManager;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class HotKeyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HotKeyDetect hotKeyDetect() {
        return new HotKeyDetect();
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeyContext HotKeyContext() {
        return new HotKeyContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientHandler ClientHandler() { return new ClientHandler(); }

    @Bean
    @ConditionalOnMissingBean
    public Notify Notify() {
        return new Notify();
    }

    @Bean
    @ConditionalOnMissingBean
    public NettyClient NettyClient() {
        return new NettyClient();
    }

    @Bean
    @ConditionalOnMissingBean
    // 配置默认的缓存管理器
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.registerCustomCache("hot", Caffeine.newBuilder()
                .expireAfterWrite(10, TimeUnit.SECONDS)
                .maximumSize(10000)
                .build());
        return cacheManager;
    }

    @Bean
    @ConditionalOnMissingBean
    public CaffeineLocalCache CaffeineLocalCache() {
        return new CaffeineLocalCache();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.register.type", havingValue = "nacos", matchIfMissing = true)
    public NacosRegister NacosRegister() {
        return new NacosRegister();
    }



}