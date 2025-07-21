package com.kuma.tools.dynamicHot.notify.netty;

import com.kuma.tools.dynamicHot.consts.Constants;
import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.notify.register.protobuf.DataModel;
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


import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

// 客户端业务处理器
@Component
@ChannelHandler.Sharable
public class ClientHandler extends SimpleChannelInboundHandler<ByteBuf> {

    @Autowired
    private HotKeyContext hotKeyContext;

    private Map<String,List<DataModel.HotKeyChunkInfo>> sessionMap = new ConcurrentHashMap<>();

    private static final Logger log = LoggerFactory.getLogger(ClientHandler.class);

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) {
        byte[] compressedBytes = new byte[msg.readableBytes()];
        msg.readBytes(compressedBytes);

        try {
            byte[] byteArray = CompressUtil.decompressToByteArray(compressedBytes);
            DataModel.HotKeyChunkInfo chunkInfo = DataModel.HotKeyChunkInfo.parseFrom(byteArray);
            if (chunkInfo.getType().equals(Constants.CHUNK_START)) {
                sessionMap.putIfAbsent(chunkInfo.getSessionId(), new ArrayList<>());
            } else if (chunkInfo.getType().equals(Constants.CHUNK_END)) {
                List<DataModel.HotKeyChunkInfo> chunkList = sessionMap.get(chunkInfo.getSessionId());
                //数据组装
                List<String> hotKeys = new ArrayList<>();
                for (DataModel.HotKeyChunkInfo chunk : chunkList) {
                    hotKeys.addAll(chunk.getDataList());
                }
                Set<String> oldKey = hotKeyContext.partitionHotKey.getOrDefault(ctx.channel().id().asShortText(), Collections.emptySet());
                Set<String> newKey = new HashSet<>(hotKeys);
                hotKeyContext.partitionHotKey.put(ctx.channel().id().asShortText(),newKey);
                hotKeyContext.globalHotKey.removeAll(oldKey);
                hotKeyContext.globalHotKey.addAll(newKey);

                sessionMap.remove(chunkInfo.getSessionId());
                log.debug("hot_key updated: {}", hotKeyContext.globalHotKey);
            } else if (chunkInfo.getType().equals(Constants.CHUNK_DATA)) {
                sessionMap.computeIfAbsent(chunkInfo.getSessionId(), k -> new ArrayList<>()).add(chunkInfo);
            }
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
                DataModel.MessageChunkInfo request = DataModel.MessageChunkInfo.newBuilder()
                        .setType(Constants.CHUNK_PING)
                        .build();
                byte[] ping = CompressUtil.compress(request.toByteArray());
                ByteBuf buffer = ctx.channel().alloc().buffer(ping.length);
                buffer.writeBytes(ping);
                ctx.writeAndFlush(buffer);
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
        Set<String> oldKey = hotKeyContext.partitionHotKey.getOrDefault(channel.id().asShortText(), Collections.emptySet());
        hotKeyContext.partitionHotKey.remove(channel.id().asShortText());
        hotKeyContext.globalHotKey.removeAll(oldKey);
    }
}
