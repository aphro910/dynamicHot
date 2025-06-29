package com.kuma.tools.dynamicHotCompute.netty;

import com.kuma.tools.dynamicHotCompute.utils.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import lombok.extern.log4j.Log4j2;


@Log4j2
public class WSServerHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        log.info("channel added, channelid: " + ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) {
        ByteBuf content = msg.content();
        try {
            byte[] bytes = new byte[content.readableBytes()];
            content.readBytes(bytes); // 读取为 byte[]
            String message = CompressUtil.decompress(bytes);
            log.info("channel received message: " + message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        log.info("channel:{} Removed...", channel.id().asLongText());
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) {

    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
}