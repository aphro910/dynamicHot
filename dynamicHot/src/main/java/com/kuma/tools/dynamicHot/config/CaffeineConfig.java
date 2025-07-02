package com.kuma.tools.dynamicHot.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CaffeineConfig {

    // 配置默认的缓存管理器
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();

        cacheManager.registerCustomCache("hot", Caffeine.newBuilder()
                .expireAfterWrite(30,TimeUnit.SECONDS)
                .maximumSize(10000)
                .build());
        return cacheManager;
    }
}
