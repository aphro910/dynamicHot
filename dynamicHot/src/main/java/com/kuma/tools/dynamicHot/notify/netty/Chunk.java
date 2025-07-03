package com.kuma.tools.dynamicHot.notify.netty;

import lombok.Data;

import java.util.List;

@Data
public class Chunk {
    private String sessionId;
    private int index;
    private int total;
    private List<String> data;
}
