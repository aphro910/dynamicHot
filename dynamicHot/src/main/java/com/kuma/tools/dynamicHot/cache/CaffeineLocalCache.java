package com.kuma.tools.dynamicHot.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class CaffeineLocalCache implements Caches{
    
    @Cacheable(value = "hot", key = "#key")
    public Object get(String key, ProceedingJoinPoint joinPoint) {
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
