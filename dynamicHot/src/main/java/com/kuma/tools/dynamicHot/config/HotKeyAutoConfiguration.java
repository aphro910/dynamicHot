package com.kuma.tools.dynamicHot.config;

import com.kuma.tools.dynamicHot.aop.HotKeyDetect;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(name = "org.aspectj.lang.annotation.Aspect")
public class HotKeyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HotKeyDetect hotKeyDetect() {
        return new HotKeyDetect();
    }
}