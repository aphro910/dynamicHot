package com.kuma.tools.dynamicHot.aspect;

import com.kuma.tools.dynamicHot.aop.DynamicHot;
import com.kuma.tools.dynamicHot.cache.Caches;
import com.kuma.tools.dynamicHot.context.HotKeyContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class HotKeyDetect {

    @Autowired
    Caches caches;
    @Autowired
    HotKeyContext hotKeyContext;

    private final ExpressionParser parser = new SpelExpressionParser();

    //环绕通知
    @Around("@annotation(com.kuma.tools.dynamicHot.aop.DynamicHot)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        //获取注解信息
        DynamicHot dynamicHot = getDynamicHotAnnotation(joinPoint);
        String key = dynamicHot.key();
        String spelExpression = dynamicHot.value();

        //解析SpEL表达式
        String dynamicKey = parseSpelExpression(joinPoint, spelExpression);

        //判断是否是hotkey
        String requestKey = key+"_"+dynamicKey;
        record(requestKey);

        if (isHot(requestKey)) {
            return caches.get(requestKey, joinPoint);
        }
        return joinPoint.proceed();
    }

    // 获取方法上的注解
    private DynamicHot getDynamicHotAnnotation(ProceedingJoinPoint joinPoint) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        return method.getAnnotation(DynamicHot.class);
    }

    private String parseSpelExpression(ProceedingJoinPoint joinPoint, String expression) {
        // 创建SpEL上下文
        StandardEvaluationContext context = new StandardEvaluationContext();

        // 设置方法参数变量
        Object[] args = joinPoint.getArgs();
        String[] paramNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();

        for (int i = 0; i < paramNames.length; i++) {
            context.setVariable(paramNames[i], args[i]);
        }

        // 设置方法名变量
        context.setVariable("methodName", joinPoint.getSignature().getName());

        // 解析表达式
        Expression exp = parser.parseExpression(expression);
        return exp.getValue(context, String.class);
    }

    private void record(String key) {
        hotKeyContext.lock.readLock().lock();
        hotKeyContext.keyMap.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
        hotKeyContext.lock.readLock().unlock();
    }

    private boolean isHot(String key) {
        return hotKeyContext.globalHotKey.contains(key);
    }
}
