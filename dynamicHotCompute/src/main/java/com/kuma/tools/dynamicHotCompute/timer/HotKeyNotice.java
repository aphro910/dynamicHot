package com.kuma.tools.dynamicHotCompute.timer;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.*;


@Component
public class HotKeyNotice {

    @Value("${spring.dynamic.hotkey.detect.timerange:60}")
    private String timerange;
    @Value("${spring.dynamic.hotkey.detect.mincount:1}")
    private String minCount;
    @Value("${spring.dynamic.hotkey.detect.maxsize:100}")
    private int maxSize;

    private Map<String, Object> params = new HashMap<>();

    @PostConstruct
    public void init() {
        // 创建桶选择器管道聚合（用于过滤）
        params.put("threshold", Double.valueOf(minCount)); // 设置阈值
    }

//    @Scheduled(initialDelay = 10000, fixedRate = 10000)
    public void notice() {
//        System.out.println("abc");
    }
}
