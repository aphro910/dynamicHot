package com.kuma.tools.dynamicHot.cache;

import cn.hutool.json.JSONUtil;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.cache.type", havingValue = "all")
public class AllCache implements Caches{

    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private RedissonClient redissonClient;
    @Value("${spring.dynamic.hotkey.cache.redis.expire:10}")
    private int expire;

    @Cacheable(value = "hot", key = "#key")
    public Object get(String key, ProceedingJoinPoint joinPoint) {
        try {
            MethodSignature signature = (MethodSignature)joinPoint.getSignature();
            Class<?> clazz = signature.getMethod().getReturnType();
            String redisKey = "cache:result:"+key;
            String res = redisTemplate.opsForValue().get(redisKey);
            if (res != null) {
                return JSONUtil.toBean(res, clazz);
            }
            //防止缓存穿透、击穿
            String lockKey = "lock:"+ key;
            RLock lock = redissonClient.getLock(lockKey);
            lock.lock();
            Object ret = joinPoint.proceed();
            if (expire > 0) {
                redisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(ret), expire, TimeUnit.SECONDS);
            } else {
                redisTemplate.opsForValue().set(redisKey, JSONUtil.toJsonStr(ret));
            }
            lock.unlock();
            return ret;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return null;
    }
}
