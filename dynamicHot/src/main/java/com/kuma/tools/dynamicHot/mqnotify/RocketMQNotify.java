package com.kuma.tools.dynamicHot.mqnotify;

import cn.hutool.json.JSONUtil;
import com.kuma.tools.dynamicHot.consts.RocketMQConsts;
import com.kuma.tools.dynamicHot.utils.CompressUtil;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "rocketmq")
public class RocketMQNotify implements MQNotify{

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void report(Map<String, Integer> map) {
        System.out.println("rocketmq reporting");
//        try {
//            byte[] compress = CompressUtil.compress(JSONUtil.toJsonStr(map));
//            rocketMQTemplate.asyncSend(RocketMQConsts.ROCKET_MQ_HOT_KEY_ANALYSIS ,compress, new SendCallback() {
//
//                @Override
//                public void onSuccess(SendResult sendResult) {
//
//                }
//
//                @Override
//                public void onException(Throwable throwable) {
//
//                }
//            });
//        } catch (Exception e) {
//            e.printStackTrace();
//        }

    }
}
