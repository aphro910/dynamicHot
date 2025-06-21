package com.kuma.tools.dynamicHot.aop;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class HotKeyDetect {

    //环绕通知
    @Around("@annotation(com.kuma.tools.dynamicHot.aspect.DynamicHot)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        System.out.println("环绕前通知");
        Object ret =  joinPoint.proceed();
        System.out.println("环绕后通知");
        return ret;
    }
}
