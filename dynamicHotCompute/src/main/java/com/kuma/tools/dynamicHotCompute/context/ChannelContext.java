package com.kuma.tools.dynamicHotCompute.context;

import cn.hutool.core.collection.ConcurrentHashSet;
import io.netty.channel.Channel;

import java.util.Set;

public class ChannelContext {

    public static Set<Channel> channels = new ConcurrentHashSet<>();
}
