package com.kuma.tools.dynamicHotCompute.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class ThreadPoolConfig {

    @Bean(value = "hotKeyComputeThreadPool")
    public ExecutorService hotKeyComputeThreadPool() {
        return Executors.newCachedThreadPool();
    }

    @Bean(value = "singleThreadPool")
    public ExecutorService singleExecutor() {
        return Executors.newSingleThreadExecutor();
    }

}
