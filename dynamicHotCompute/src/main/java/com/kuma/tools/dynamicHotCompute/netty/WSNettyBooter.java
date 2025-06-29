package com.kuma.tools.dynamicHotCompute.netty;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/*netty websocket starter*/
@Component
public class WSNettyBooter {

    @Value("${spring.server.netty.port:8088}")
    private int port;

    private WSNettyServer nettyServer;

    @PostConstruct
    public void onBoot() {
        try {
            nettyServer = WSNettyServer.getInstance();
            nettyServer.start(port);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void onStop() {
        nettyServer.stop();
    }
}
