package com.kuma.tools.dynamicHot.cache;

import org.aspectj.lang.ProceedingJoinPoint;

public interface Caches {

    Object get(String key, ProceedingJoinPoint joinPoint);
}
