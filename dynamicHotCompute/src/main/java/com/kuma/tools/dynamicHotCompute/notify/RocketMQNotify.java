package com.kuma.tools.dynamicHotCompute.notify;

import com.kuma.tools.dynamicHotCompute.consts.RocketMQConsts;
import com.kuma.tools.dynamicHotCompute.utils.CompressUtil;
import org.apache.rocketmq.client.producer.SendCallback;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "spring.dynamic.hotkey.mq.type", havingValue = "rocketmq")
public class RocketMQNotify implements MQNotify {

    @Autowired
    private RocketMQTemplate rocketMQTemplate;

    @Override
    public void notice(String msg) {
        try {
            byte[] compress = CompressUtil.compress(msg);
            rocketMQTemplate.asyncSend(RocketMQConsts.ROCKET_MQ_HOTKEY_DETECT_BROADCAST,compress, new SendCallback() {

                @Override
                public void onSuccess(SendResult sendResult) {

                }

                @Override
                public void onException(Throwable throwable) {

                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
