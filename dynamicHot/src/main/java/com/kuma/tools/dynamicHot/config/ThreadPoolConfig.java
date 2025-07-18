package com.kuma.tools.dynamicHot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

@Configuration
public class ThreadPoolConfig {

    @Bean(value = "singleScheduledThreadPool")
    public ScheduledExecutorService singleScheduledThreadPool() {
        return Executors.newSingleThreadScheduledExecutor();
    }
}
