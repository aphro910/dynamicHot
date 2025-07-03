package com.kuma.tools.dynamicHotCompute.entity;

import lombok.Data;

@Data
public class ChunkInfo {
    private String type;//START、END
    private String sessionId;
    private int chunkSize;
}
