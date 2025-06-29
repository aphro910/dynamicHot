package com.kuma.tools.dynamicHotCompute.netty;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import lombok.extern.log4j.Log4j2;

@Log4j2
public class WSNettyServer {

    private static class SingletionWSServer {
        static final WSNettyServer instance = new WSNettyServer();
    }

    public static WSNettyServer getInstance() {
        return SingletionWSServer.instance;
    }

    private EventLoopGroup mainGroup;
    private EventLoopGroup subGroup;
    private ServerBootstrap server;
    private ChannelFuture future;

    public WSNettyServer() {
        mainGroup = new NioEventLoopGroup(1);
        subGroup = new NioEventLoopGroup(5);
        server = new ServerBootstrap();
    }

    public void start(int port) {
        server.group(mainGroup, subGroup)
                .channel(NioServerSocketChannel.class)
                .localAddress(port)
                .childHandler(new WSServerChannelInitialzer());

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
