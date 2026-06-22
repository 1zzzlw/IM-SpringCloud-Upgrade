package com.zzzlew.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.zzzlew.constant.RabbitMQConstant.*;

/**
 * 打赏相关队列声明（im-pay 端负责声明两个队列并绑定到共用 Exchange）
 */
@Configuration
public class RewardQueueConfig {

    @Bean
    public Queue rewardCreateQueue() {
        return QueueBuilder.durable(QUEUE_REWARD_CREATE).build();
    }

    @Bean
    public Queue rewardResultQueue() {
        return QueueBuilder.durable(QUEUE_REWARD_RESULT).build();
    }

    @Bean
    public Binding rewardCreateBinding(Queue rewardCreateQueue,
                                       @Qualifier("imTopicExchange") TopicExchange exchange) {
        return BindingBuilder.bind(rewardCreateQueue).to(exchange).with(ROUTING_KEY_REWARD_CREATE);
    }

    @Bean
    public Binding rewardResultBinding(Queue rewardResultQueue,
                                       @Qualifier("imTopicExchange") TopicExchange exchange) {
        return BindingBuilder.bind(rewardResultQueue).to(exchange).with(ROUTING_KEY_REWARD_RESULT);
    }

    @Bean
    public Queue redpacketGrabQueue() {
        return QueueBuilder.durable(QUEUE_REDPACKET_GRAB).build();
    }

    @Bean
    public Binding redpacketGrabBinding(Queue redpacketGrabQueue,
                                        @Qualifier("imTopicExchange") TopicExchange exchange) {
        return BindingBuilder.bind(redpacketGrabQueue).to(exchange).with(ROUTING_KEY_REDPACKET_GRAB);
    }
}
