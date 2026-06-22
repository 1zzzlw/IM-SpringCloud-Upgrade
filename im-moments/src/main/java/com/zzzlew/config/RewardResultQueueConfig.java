package com.zzzlew.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.zzzlew.constant.RabbitMQConstant.*;

/**
 * im-moments 端声明打赏结果队列，绑定到共用 Exchange
 */
@Configuration
public class RewardResultQueueConfig {

    @Bean
    public Queue momentRewardResultQueue() {
        return QueueBuilder.durable(QUEUE_REWARD_RESULT).build();
    }

    @Bean
    public Binding momentRewardResultBinding(Queue momentRewardResultQueue,
                                              @Qualifier("imTopicExchange") TopicExchange exchange) {
        return BindingBuilder.bind(momentRewardResultQueue).to(exchange).with(ROUTING_KEY_REWARD_RESULT);
    }
}
