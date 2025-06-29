package com.kuma.tools.dynamicHot.notify.netty;

import com.kuma.tools.dynamicHot.utils.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

// 客户端业务处理器
public class ClientHandler extends SimpleChannelInboundHandler<String> {

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, String msg) {
        System.out.println("Received from server: " + msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent event = (IdleStateEvent) evt;
            //发送心跳消息
            try {
                byte[] ping = CompressUtil.compress("pingpong");
                ByteBuf buffer = Unpooled.wrappedBuffer(ping);
                BinaryWebSocketFrame binaryWebSocketFrame = new BinaryWebSocketFrame(buffer);
                ctx.writeAndFlush(binaryWebSocketFrame);
            } catch (Exception e) {
                e.printStackTrace();
            }

        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        NettyClient.remove(channel);
        log.info("channel:{} Removed", channel.id());
    }
}
