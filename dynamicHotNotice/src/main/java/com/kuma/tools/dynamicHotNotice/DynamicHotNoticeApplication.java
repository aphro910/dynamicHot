package com.kuma.tools.dynamicHotNotice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DynamicHotNoticeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicHotNoticeApplication.class, args);
    }

}
