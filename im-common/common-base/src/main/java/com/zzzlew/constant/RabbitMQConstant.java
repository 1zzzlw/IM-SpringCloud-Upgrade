package com.zzzlew.constant;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/24 - 03 - 24 - 17:29
 * @Description: com.zzzlew.constant
 * @version: 1.0
 */
public class RabbitMQConstant {

    // 消息业务的交换机
    public static final String EXCHANGE = "im-topic-exchange";
    // Netty集群的消息队列
    public static final String QUEUE_NETTY_PREFIX = "im-push-queue-";
    // Netty集群的消息队列的路由规则
    public static final String QUEUE_NETTY_ROUTING_KEY= "im.push.";
    // SpringBoot监听的异步存储的消息队列
    public static final String QUEUE_STORGE_PREFIX = "im-storage-queue";
    // SpringBoot监听的异步存储的消息队列的路由规则
    public static final String QUEUE_STORGE_ROUTING_KEY = "im.storage";
}
