package com.kuma.tools.dynamicHot.aspect;

import com.kuma.tools.dynamicHot.aop.DynamicHot;
import com.kuma.tools.dynamicHot.context.HotKeyContext;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
public class HotKeyDetect {

    private final ExpressionParser parser = new SpelExpressionParser();

    //环绕通知
    @Around("@annotation(com.kuma.tools.dynamicHot.aop.DynamicHot)")
    public Object around(ProceedingJoinPoint joinPoint) throws Throwable {
        // 1. 获取注解信息
        DynamicHot dynamicHot = getDynamicHotAnnotation(joinPoint);
        String tableName = dynamicHot.tableName();
        String spelExpression = dynamicHot.id();

        // 2. 解析SpEL表达式
        String dynamicKey = parseSpelExpression(joinPoint, spelExpression);

        System.out.println("表名: " + tableName + ", 动态键: " + dynamicKey);
        //todo 判断是否是hotkey
        Object ret =  joinPoint.proceed();
        String key = tableName+"_"+dynamicKey;
        record(key);
        return ret;
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
        while (HotKeyContext.flag == 1) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        HotKeyContext.hotKeyMap.compute(key, (k, v) -> (v == null) ? 1 : v + 1);
    }

}
