package com.kuma.tools.dynamicHot.config;

import com.kuma.tools.dynamicHot.aspect.HotKeyDetect;
import com.kuma.tools.dynamicHot.cache.AllCache;
import com.kuma.tools.dynamicHot.cache.CaffeineLocalCache;
import com.kuma.tools.dynamicHot.cache.RedisCache;
import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.notify.Notify;
import com.kuma.tools.dynamicHot.notify.netty.ClientHandler;
import com.kuma.tools.dynamicHot.notify.netty.NettyClient;
import com.kuma.tools.dynamicHot.notify.register.NacosRegister;
import com.kuma.tools.dynamicHot.timer.HotKeyReport;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HotKeyAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public HotKeyDetect hotKeyDetect() {
        return new HotKeyDetect();
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeyReport HotKeyReport() {
        return new HotKeyReport();
    }

    @Bean
    @ConditionalOnMissingBean
    public HotKeyContext HotKeyContext() {
        return new HotKeyContext();
    }

    @Bean
    @ConditionalOnMissingBean
    public ClientHandler ClientHandler() { return new ClientHandler(); }

    @Bean
    @ConditionalOnMissingBean
    public Notify Notify() {
        return new Notify();
    }

    @Bean
    @ConditionalOnMissingBean
    public NettyClient NettyClient() {
        return new NettyClient();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.register.type", havingValue = "nacos", matchIfMissing = true)
    public NacosRegister NacosRegister() {
        return new NacosRegister();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.cache.type", havingValue = "local", matchIfMissing = true)
    public CaffeineLocalCache CaffeineLocalCache() {
        return new CaffeineLocalCache();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.cache.type", havingValue = "redis")
    public RedisCache RedisCache() {
        return new RedisCache();
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(name = "spring.dynamic.hotkey.cache.type", havingValue = "all")
    public AllCache AllCache() {
        return new AllCache();
    }


}