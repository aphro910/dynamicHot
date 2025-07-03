package com.kuma.tools.dynamicHot.notify.netty;

import lombok.Data;

@Data
public class ChunkInfo {
    private String type;//START、END
    private String sessionId;
    private int chunkSize;
}
