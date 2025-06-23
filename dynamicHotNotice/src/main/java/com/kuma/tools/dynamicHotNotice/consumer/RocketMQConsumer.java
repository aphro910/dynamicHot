package com.kuma.tools.dynamicHotNotice.consumer;


import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.kuma.tools.dynamicHotNotice.mapper.ESDynamicHotMapper;
import com.kuma.tools.dynamicHotNotice.consts.RocketMQConsts;
import com.kuma.tools.dynamicHotNotice.entity.ESDynamicHot;
import com.kuma.tools.dynamicHotNotice.utils.CompressUtil;

import com.kuma.tools.dynamicHotNotice.utils.SnowFlakeGenerator;
import org.apache.rocketmq.client.consumer.DefaultMQPushConsumer;
import org.apache.rocketmq.client.consumer.listener.*;
import org.apache.rocketmq.common.message.MessageExt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

@Component
public class RocketMQConsumer {

    @Value("${rocketmq.name-server}")
    private String serverAddr;
    @Autowired
    private ESDynamicHotMapper esDynamicHotMapper;
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    private DefaultMQPushConsumer consumer = new DefaultMQPushConsumer("hot_key_detect_consumer");

    @PostConstruct
    public void start() {
        try {
            // 指定Namesrv地址信息.
            consumer.setNamesrvAddr(serverAddr);
            // 订阅Topic
            consumer.subscribe(RocketMQConsts.ROCKET_MQ_HOT_KEY_ANALYSIS, "*");
//            consumer.setConsumeMessageBatchMaxSize(100);//一次拉取的消息数量，默认是1
            consumer.registerMessageListener(new MessageListenerConcurrently() {
                @Override
                public ConsumeConcurrentlyStatus consumeMessage(List<MessageExt> list, ConsumeConcurrentlyContext consumeConcurrentlyContext) {
                    try {
                        Date time = new Date();
                        List<ESDynamicHot> batchList = new ArrayList<>();
                        for (MessageExt message : list) {
                            String msg = CompressUtil.decompress(message.getBody());
                            JSONObject jsonObject = JSONUtil.parseObj(msg);
                            Set<String> keySet = jsonObject.keySet();
                            for (String key : keySet) {
                                ESDynamicHot esDynamicHot = new ESDynamicHot();
                                int count = jsonObject.getInt(key);
                                esDynamicHot.setId(SnowFlakeGenerator.nextId());
                                esDynamicHot.setKey(key);
                                esDynamicHot.setCount(count);
                                esDynamicHot.setTime(time);
                                batchList.add(esDynamicHot);
                            }
                        }
                        System.out.println(batchList);
//                        esDynamicHotMapper.saveAll(batchList);
                        return ConsumeConcurrentlyStatus.CONSUME_SUCCESS;
                    } catch (Exception e) {
                        return ConsumeConcurrentlyStatus.RECONSUME_LATER;
                    }
                }
            });
            consumer.start();
            System.out.println("start consumer ....");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @PreDestroy
    public void stop() {
        consumer.shutdown();
    }
}
