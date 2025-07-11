package com.kuma.tools.dynamicHotCompute.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Component
public class WSNettyServer {

    @Autowired
    private WSServerChannelInitialzer serverChannelInitialzer;

    @Value("${spring.server.netty.port:8088}")
    private int port;

    private static final Logger log = LoggerFactory.getLogger(WSNettyServer.class);

    private EventLoopGroup mainGroup;
    private EventLoopGroup subGroup;
    private ServerBootstrap server;
    private ChannelFuture future;

    @PostConstruct
    public void init() {
        mainGroup = new NioEventLoopGroup(1);
        subGroup = new NioEventLoopGroup(5);
        server = new ServerBootstrap();
        start(port);
    }

    public void start(int port) {
        server.group(mainGroup, subGroup)
                .channel(NioServerSocketChannel.class)
                .localAddress(port)
                .childHandler(serverChannelInitialzer);

        this.future = server.bind().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.info("netty server 启动完毕 port:{}", port);
                } else {
                    log.error("netty server 启动失败...");
                }
            }
        });
    }

    @PreDestroy
    public void stop() {
        this.future.channel().closeFuture().addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture channelFuture) throws Exception {
                if (channelFuture.isSuccess()) {
                    log.info("netty server 已关闭...");
                } else {
                    log.error("netty server 关闭失败...");
                }
            }
        });
        mainGroup.shutdownGracefully();
        subGroup.shutdownGracefully();
    }
}
