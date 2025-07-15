package com.kuma.tools.dynamicHot.notify.netty;

import com.kuma.tools.dynamicHot.consts.Constants;
import com.kuma.tools.dynamicHot.notify.register.protobuf.DataModel;
import com.kuma.tools.dynamicHot.utils.CompressUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.websocketx.BinaryWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketClientProtocolHandler;
import io.netty.handler.codec.http.websocketx.WebSocketVersion;
import io.netty.handler.stream.ChunkedWriteHandler;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PreDestroy;
import java.io.IOException;
import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class NettyClient {

    @Autowired
    private ClientHandler clientHandler;

    private static final Logger log = LoggerFactory.getLogger(NettyClient.class);
    private static Map<String, Channel> connections = new ConcurrentHashMap<>();
    private EventLoopGroup eventLoopGroup = new NioEventLoopGroup(5);

    private List<String> hostList = new ArrayList<>();
    private List<List<DataModel.Message>> batchList;

    private static final int CHUNK_SIZE = 1000;

    public static void remove(Channel channel) {
        for (Map.Entry<String, Channel> entry : connections.entrySet()) {
            Channel ch = entry.getValue();
            if (ch.id().equals(channel.id())) {
                connections.remove(entry.getKey());
                log.info("remove channel {}", entry.getKey());
                return;
            }
        }
    }

    public void connect(String host, int port) {
        if (connections.containsKey(host)) {
            return;
        }
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    protected void initChannel(SocketChannel ch) {
                        ChannelPipeline pipeline = ch.pipeline();
                        pipeline.addLast(new IdleStateHandler(60, 60, 0));
                        // 1. HTTP 编解码器
                        pipeline.addLast(new HttpClientCodec());
                        // 2. 聚合 HTTP 消息
                        pipeline.addLast(new HttpObjectAggregator(65536));
                        // 3. 处理大文件/分块传输
                        pipeline.addLast(new ChunkedWriteHandler());
                        // 4. WebSocket 协议处理器
                        pipeline.addLast(new WebSocketClientProtocolHandler(
                                URI.create("ws://" + host + ":" + port + "/ws"),
                                WebSocketVersion.V13,
                                null,
                                true,
                                new DefaultHttpHeaders(),
                                65536
                        ));
                        // 添加业务处理器
                        pipeline.addLast(clientHandler);

                    }
                });
        try {
            ChannelFuture future = bootstrap.connect(host, port).sync();
            if (future.isSuccess()) {
                Channel channel = future.channel();
                connections.put(host, channel);
                log.info("Connected to new server: " + host + ":" + port);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public void send(Map<String, Integer> data) {
        Set<String> keySet = connections.keySet();
        long timestamp = System.currentTimeMillis();
        if (hostList.size() != keySet.size()) {
            hostList = keySet.stream().sorted().collect(Collectors.toList());
            batchList = new ArrayList<>();
            for (int i = 0; i < hostList.size(); i++) {
                batchList.add(new ArrayList<>());
            }
        }
        if (hostList.isEmpty() || batchList.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : data.entrySet()) {
            String key = entry.getKey();
            Integer count = entry.getValue();
            int hash = Math.abs(key.hashCode()) % batchList.size();
            DataModel.Message message = DataModel.Message.newBuilder()
                    .setKey(key)
                    .setCount(count)
                    .setTimestamp(timestamp)
                    .build();
            batchList.get(hash).add(message);
        }

        for (int i = 0; i < batchList.size(); i++) {
            batchSendInChunks(batchList.get(i), i);
        }

        clearBatchList();
    }

    private void clearBatchList() {
        for (List<DataModel.Message> messages : batchList) {
            messages.clear();
        }
    }

    private void batchSendInChunks(List<DataModel.Message> list, int index) {
        Channel channel = connections.get(hostList.get(index));
        int totalChunks = (int) Math.ceil((double) list.size() / CHUNK_SIZE);
        for (int i = 0; i < totalChunks; i++) {
            int start = i * CHUNK_SIZE;
            int end = Math.min(start + CHUNK_SIZE, list.size());
            List<DataModel.Message> chunkList = list.subList(start, end);

            // 构建分块数据
            DataModel.MessageChunkInfo chunkData = DataModel.MessageChunkInfo.newBuilder()
                    .setType(Constants.CHUNK_DATA)
                    .addAllBatchMessage(chunkList)
                    .build();

            try {
                // 压缩并发送
                byte[] compressed = CompressUtil.compress(chunkData.toByteArray());
                ByteBuf buffer = Unpooled.wrappedBuffer(compressed);
                channel.writeAndFlush(new BinaryWebSocketFrame(buffer));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @PreDestroy
    public void destroy() {
        eventLoopGroup.shutdownGracefully();
    }
}