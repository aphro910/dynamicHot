package com.kuma.tools.dynamicHotCompute;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class DynamicHotComputeApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicHotComputeApplication.class, args);
    }

}
