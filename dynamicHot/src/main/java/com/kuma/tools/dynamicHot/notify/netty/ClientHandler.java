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
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// 客户端业务处理器
@Component
@ChannelHandler.Sharable
public class ClientHandler extends SimpleChannelInboundHandler<WebSocketFrame> {

    @Autowired
    private HotKeyContext hotKeyContext;

    private Map<String,List<Chunk>> sessionMap = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, WebSocketFrame msg) {
        if (msg instanceof BinaryWebSocketFrame) {
            ByteBuf content = msg.content();
            byte[] bytes = new byte[content.readableBytes()];
            content.readBytes(bytes); // 读取为 byte[]
            try {
                String str = CompressUtil.decompress(bytes);
                Chunk chunk = JSONUtil.toBean(str, Chunk.class);
                sessionMap.computeIfAbsent(chunk.getSessionId(), k -> new ArrayList<>()).add(chunk);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else if (msg instanceof TextWebSocketFrame) {
            String content = ((TextWebSocketFrame) msg).text();
            ChunkInfo chunkInfo = JSONUtil.toBean(content, ChunkInfo.class);
            if (chunkInfo.getType().equals(Constants.CHUNK_START)) {
                sessionMap.putIfAbsent(chunkInfo.getSessionId(), new ArrayList<>());
            } else {
                List<Chunk> chunkList = sessionMap.get(chunkInfo.getSessionId());
                //数据组装
                List<String> hotKeys = new ArrayList<>();
                for (Chunk chunk : chunkList) {
                    hotKeys.addAll(chunk.getData());
                }

                hotKeyContext.hotKey.put(ctx.channel().id().asShortText(),new HashSet<>(hotKeys));
                log.info("hot_key updated: {}", hotKeyContext.hotKey);
            }
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
        hotKeyContext.hotKey.remove(channel.id().asShortText());
        log.info("channel:{} Removed", channel.id());
    }
}
