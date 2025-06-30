package com.kuma.tools.dynamicHotCompute.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpServerCodec;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import io.netty.handler.stream.ChunkedWriteHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class WSServerChannelInitialzer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private WSServerHandler wsServerHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();

        pipeline.addLast(new HttpServerCodec());
        pipeline.addLast(new HttpObjectAggregator(65536));
        pipeline.addLast(new ChunkedWriteHandler());
        //WebSocketServerProtocolHandler在最开始http升级为websocket协议后将http相关解码器移除并替换为websocketflame解码器
        //websocket本身是应用层协议，帧头已包括数据帧大小，因此无需显式地处理粘包拆包
        pipeline.addLast(new WebSocketServerProtocolHandler("/ws"));

        // 添加自定义的处理器
        pipeline.addLast(wsServerHandler);
    }

}
