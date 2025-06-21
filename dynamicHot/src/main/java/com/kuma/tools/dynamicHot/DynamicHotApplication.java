package com.kuma.tools.dynamicHot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@SpringBootApplication
@EnableAspectJAutoProxy
@ComponentScan(basePackages = "com.kuma.tools.dynamicHot.*")
public class DynamicHotApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicHotApplication.class, args);
    }

}
