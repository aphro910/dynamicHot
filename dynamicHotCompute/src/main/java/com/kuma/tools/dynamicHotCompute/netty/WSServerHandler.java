package com.kuma.tools.dynamicHotCompute.netty;

import cn.hutool.json.JSONUtil;
import com.kuma.tools.dynamicHotCompute.consts.Constants;
import com.kuma.tools.dynamicHotCompute.context.ChannelContext;
import com.kuma.tools.dynamicHotCompute.handler.HotKeyHandler;
import com.kuma.tools.dynamicHotCompute.entity.Message;
import com.kuma.tools.dynamicHotCompute.utils.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Log4j2
@Component
@ChannelHandler.Sharable
public class WSServerHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    @Autowired
    HotKeyHandler hotKeyHandler;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        ChannelContext.channelGroup.add(ctx.channel());
        log.info("channel added, channel: " + ctx.channel());
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) {
        ByteBuf content = msg.content();
        try {
            byte[] bytes = new byte[content.readableBytes()];
            content.readBytes(bytes); // 读取为 byte[]
            String str = CompressUtil.decompress(bytes);
            if (str.equals(Constants.PING_PONG)) {
                return;
            }
            log.info("channel received message: " + str);
            Message message = JSONUtil.toBean(str, Message.class);
            hotKeyHandler.add(message);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) {
        Channel channel = ctx.channel();
        ChannelContext.channelGroup.remove(channel);
        log.info("channel:{} Removed...", channel.id());
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