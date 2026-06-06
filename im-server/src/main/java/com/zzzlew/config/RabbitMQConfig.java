package com.zzzlew.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.zzzlew.constant.RabbitMQConstant.EXCHANGE;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/3
 * @Description: RabbitMQ 配置类
 * @version: 1.0
 */
@Slf4j
@Configuration
public class RabbitMQConfig {

    /**
     * 定义共用的 Topic 交换机
     */
    @Bean(name = "imTopicExchange")
    public TopicExchange imTopicExchange() {
        return ExchangeBuilder.topicExchange(EXCHANGE).durable(true).build();
    }

    /**
     * 消息转换器，使用 Jackson 进行 JSON 序列化
     */
    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    /**
     * RabbitAdmin 用于声明队列、交换机和绑定关系
     */
    @Bean
    public RabbitAdmin rabbitAdmin(ConnectionFactory factory) {
        return new RabbitAdmin(factory);
    }
}
