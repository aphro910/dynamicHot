package com.kuma.tools.dynamicHotCompute.netty;

import com.kuma.tools.dynamicHotCompute.context.ChannelContext;
import com.kuma.tools.dynamicHotCompute.handler.HotKeyHandler;
import com.kuma.tools.dynamicHotCompute.protobuf.DataModel;
import com.kuma.tools.dynamicHotCompute.utils.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@ChannelHandler.Sharable
public class WSServerHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    @Autowired
    HotKeyHandler hotKeyHandler;

    private static final Logger log = LoggerFactory.getLogger(WSServerHandler.class);

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
            byte[] res = CompressUtil.decompressToByteArray(bytes);
            DataModel.Message message = DataModel.Message.parseFrom(res);
            if (message.getPingpong()) {
                return;
            }
            log.debug("channel received message: {}" , message);
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