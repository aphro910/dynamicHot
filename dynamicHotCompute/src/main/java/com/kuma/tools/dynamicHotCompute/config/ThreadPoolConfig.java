package com.kuma.tools.dynamicHotCompute.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ThreadPoolConfig {

    @Bean(value = "hotKeyComputeThreadPool")
    public ExecutorService hotKeyComputeThreadPool() {
        return Executors.newCachedThreadPool();
    }

    @Bean(value = "singleScheduledThreadPool")
    public ScheduledExecutorService singleScheduledThreadPool() {
        return Executors.newSingleThreadScheduledExecutor();
    }

    @Bean(value = "hotkeySaveThreadPool")
    public ScheduledExecutorService hotkeySaveThreadPool() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
