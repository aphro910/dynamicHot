package com.kuma.tools.dynamicHotCompute.entity;

import lombok.Data;

@Data
public class Message {
    private String key;
    private Integer count;
    private long timestamp;
}
