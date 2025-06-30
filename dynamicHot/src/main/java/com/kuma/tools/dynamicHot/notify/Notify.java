package com.kuma.tools.dynamicHot.notify;

import com.kuma.tools.dynamicHot.notify.netty.NettyClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class Notify {

    @Autowired
    NettyClient nettyClient;

    public void send(Map<String, Integer> data) {
        nettyClient.send(data);
    }
}
