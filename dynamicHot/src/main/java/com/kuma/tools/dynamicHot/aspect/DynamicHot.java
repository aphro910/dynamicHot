package com.kuma.tools.dynamicHot.aspect;


import java.lang.annotation.*;

@Target(ElementType.METHOD) // 表示注解只能用在方法上
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface DynamicHot {
    // 可定义注解的属性（可选）
    String tableName();
    String id();
}
