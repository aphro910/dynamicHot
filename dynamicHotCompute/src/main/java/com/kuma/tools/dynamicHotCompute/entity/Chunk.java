package com.kuma.tools.dynamicHotCompute.entity;

import lombok.Data;

import java.util.List;

@Data
public class Chunk {
    private String sessionId;
    private int index;
    private int total;
    private List<String> data;
}
