package com.kuma.tools.dynamicHot.notify.netty;

import cn.hutool.json.JSONUtil;
import com.kuma.tools.dynamicHot.consts.Constants;
import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.utils.CompressUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;

// 客户端业务处理器
@Component
@ChannelHandler.Sharable
public class ClientHandler extends SimpleChannelInboundHandler<BinaryWebSocketFrame> {

    @Autowired
    private HotKeyContext hotKeyContext;

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, BinaryWebSocketFrame msg) {
        ByteBuf content = msg.content();
        try {
            byte[] bytes = new byte[content.readableBytes()];
            content.readBytes(bytes); // 读取为 byte[]
            String str = CompressUtil.decompress(bytes);
            List<String> hotKeyList = JSONUtil.toList(JSONUtil.parseArray(str), String.class);
            hotKeyContext.hotKey = new HashSet<>(hotKeyList);
            log.info("hot_key updated: {}", hotKeyContext.hotKey);
        } catch (Exception e) {
            e.printStackTrace();
        }
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
                byte[] ping = CompressUtil.compress(Constants.PING_PONG);
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
