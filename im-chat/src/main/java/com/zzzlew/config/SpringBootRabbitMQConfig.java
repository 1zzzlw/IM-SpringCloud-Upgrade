package com.zzzlew.config;

import jakarta.annotation.Resource;
import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.zzzlew.constant.RabbitMQConstant.QUEUE_STORGE_PREFIX;
import static com.zzzlew.constant.RabbitMQConstant.QUEUE_STORGE_ROUTING_KEY;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/24 - 03 - 24 - 11:11
 * @Description: com.zzzlew.config
 * @version: 1.0
 */
@Configuration
public class SpringBootRabbitMQConfig {
    @Resource
    @Qualifier("imTopicExchange")
    private TopicExchange imTopicExchange;

    // 创建固定的存库队列
    @Bean
    public Queue imStorageQueue() {
        return QueueBuilder.durable(QUEUE_STORGE_PREFIX).lazy().build();
    }

    // 绑定到Topic交换机，路由键用im.storage
    @Bean
    public Binding storageBinding() {
        return BindingBuilder
                .bind(imStorageQueue())
                .to(imTopicExchange)
                .with(QUEUE_STORGE_ROUTING_KEY);
    }
}