package com.archie.sso.authorize.server.listener.mq;

import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * @author lavyoung1325 <2034549297@qq.com>
 * Created on 2025-03-16
 */
@Component
@Slf4j
@ConditionalOnProperty(prefix = "iam.messaging.rocketmq", name = "enabled", havingValue = "true", matchIfMissing = true)
@RocketMQMessageListener(topic = "cam-authorization-server-topic", consumerGroup = "cam-authorization-server-c1")
public class CamTopicListener implements RocketMQListener<String> {

    @Value("${rocketmq.topic}")
    private String topic;


    @Override
    public void onMessage(String message) {

        log.info("开始消费：topic:{}", topic);
        log.info(message);
    }
}
