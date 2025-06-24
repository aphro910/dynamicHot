package com.kuma.tools.dynamicHot.consumer;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.kuma.tools.dynamicHot.consts.RocketMQConsts;
import com.kuma.tools.dynamicHot.context.HotKeyContext;
import com.kuma.tools.dynamicHot.utils.CompressUtil;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext;
import org.apache.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import org.apache.rocketmq.client.consumer.listener.MessageListenerConcurrently;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.common.protocol.heartbeat.MessageModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.HashSet;
import java.util.List;


@Component
@Log4j2
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "rocketmq")
public class RocketMQConsumer implements HotKeyMQConsumer {

    @Value("${rocketmq.name-server}")
    private String serverAddr;

    private DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("hot_key_notice_consumer");

    @PostConstruct
    public void start() {
        try {
            // 指定Namesrv地址信息.
            consumer.setNamesrvAddr(serverAddr);
            // 订阅Topic
            consumer.subscribe(RocketMQConsts.ROCKET_MQ_HOTKEY_DETECT_BROADCAST, "*");
            consumer.setMessageModel(MessageModel.BROADCASTING);//消息模式为广播
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                    try {
                        for (MessageExt message : list) {
                            String msg = CompressUtil.decompress(message.getBody());
                            List<String> hotList = JSONUtil.toList(JSONUtil.parseArray(msg),String.class);
                            HotKeyContext.hotKey = new HashSet<>(hotList);
                        }
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    } catch (Exception e) {
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
            });
            consumer.start();
            log.info("hotkey consumer started successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stop() {
        consumer.shutdown();
    }
}
