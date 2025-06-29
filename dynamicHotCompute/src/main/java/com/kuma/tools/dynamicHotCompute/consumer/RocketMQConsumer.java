package com.kuma.tools.dynamicHotCompute.consumer;


import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kuma.tools.dynamicHotCompute.mapper.ESDynamicHotMapper;
import com.kuma.tools.dynamicHotCompute.consts.RocketMQConsts;
import com.kuma.tools.dynamicHotCompute.entity.ESDynamicHot;
import com.kuma.tools.dynamicHotCompute.utils.CompressUtil;

import com.kuma.tools.dynamicHotCompute.utils.SnowFlakeGenerator;
import lombok.extern.log4j.Log4j2;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
@Log4j2
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "rocketmq")
public class RocketMQConsumer implements HotKeyMQConsumer {

    @Value("${rocketmq.name-server}")
    private String serverAddr;
    @Autowired
    private ESDynamicHotMapper esDynamicHotMapper;

    private DefaultMQPushConsumer detectConsumer = new DefaultMQPushConsumer("hot_key_detect_consumer");

    @PostConstruct
    public void start() {
        try {
            // 指定Namesrv地址信息.
            detectConsumer.setNamesrvAddr(serverAddr);
            // 订阅Topic
            detectConsumer.subscribe(RocketMQConsts.ROCKET_MQ_HOT_KEY_ANALYSIS, "*");
            detectConsumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                    try {
                        List<ESDynamicHot> batchList = new ArrayList<>();
                        for (MessageExt message : list) {
                            String msg = CompressUtil.decompress(message.getBody());
                            log.info("msg received:{}", msg);
                            JSONObject obj = JSONUtil.parseObj(msg);
                            long timestamp = obj.getLong("timestamp");
                            JSONObject jsonObject = obj.getJSONObject("key");
                            Set<String> keySet = jsonObject.keySet();
                            for (String key : keySet) {
                                ESDynamicHot esDynamicHot = new ESDynamicHot();
                                int count = jsonObject.getInt(key);
                                esDynamicHot.setId(SnowFlakeGenerator.nextId());
                                esDynamicHot.setKey(key);
                                esDynamicHot.setCount(count);
                                esDynamicHot.setTime(new Date(timestamp));
                                batchList.add(esDynamicHot);
                            }
                        }
                        esDynamicHotMapper.saveAll(batchList);
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    } catch (Exception e) {
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
            });
            detectConsumer.start();
            log.debug("hotkey consumer started successfully");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stop() {
        detectConsumer.shutdown();
    }
}
