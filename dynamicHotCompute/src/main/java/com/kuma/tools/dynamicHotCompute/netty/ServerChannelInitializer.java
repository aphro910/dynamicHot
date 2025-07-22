package com.kuma.tools.dynamicHotCompute.netty;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ServerChannelInitializer extends ChannelInitializer<SocketChannel> {

    @Autowired
    private ServerHandler serverHandler;

    @Override
    protected void initChannel(SocketChannel ch) {
        ChannelPipeline pipeline = ch.pipeline();
        pipeline.addLast(new LengthFieldBasedFrameDecoder(
                1024 * 1024,
                0,    // 长度字段偏移量
                4,    // 长度字段长度 (4字节 = int32)
                0,    // 长度调整值
                4     // 剥离头部字节数
        ));
        //长度字段编码器 (添加4字节长度前缀)
        pipeline.addLast(new LengthFieldPrepender(4));
        // 添加自定义的处理器
        pipeline.addLast(serverHandler);

    }

}
