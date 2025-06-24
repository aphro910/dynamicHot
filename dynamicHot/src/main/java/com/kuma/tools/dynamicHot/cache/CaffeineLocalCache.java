package com.kuma.tools.dynamicHot.cache;

import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

@Component
public class CaffeineLocalCache {
    
    @Cacheable(value = "hot", key = "#key")
    public Object getRet(String key, ProceedingJoinPoint joinPoint) {
        try {
            System.out.println(key);
            return joinPoint.proceed();
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
