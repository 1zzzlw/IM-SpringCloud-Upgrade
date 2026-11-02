package com.zzzlew.handler;


import cn.hutool.extra.spring.SpringUtil;
import com.zzzlew.domain.Message;
import com.zzzlew.handler.impl.MessageHandler;
import com.zzzlew.handler.messageHandler.*;
import com.zzzlew.publish.MQMessagePublish;
import com.zzzlew.result.MessageResult;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/19 - 11 - 19 - 21:00
 * @Description: com.zzzlew.zzzimserver.handler
 * @version: 1.0
 */
@Slf4j
@ChannelHandler.Sharable
public class MessageDispatcherHandler extends ChannelInboundHandlerAdapter {

    private MQMessagePublish mqMessagePublish;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        if (this.mqMessagePublish == null) {
            this.mqMessagePublish = SpringUtil.getBean(MQMessagePublish.class);
        }
    }

    // 存放 消息类型 和 Handler 的映射，方便分发给对应的 Handler 处理
    private final Map<Integer, MessageHandler> handlerMap;

    public MessageDispatcherHandler() {
        handlerMap = new ConcurrentHashMap<>();
        // 注册消息类型与对应的 Handler
        handlerMap.put(Message.PrivateChatRequestDTO, new PrivateChatHandler());
        handlerMap.put(Message.GroupChatRequestDTO, new GroupChatHandler());
        handlerMap.put(Message.FriendApplyRequestDTO, new FriendApplySendHandler());
        handlerMap.put(Message.GroupApplyRequestDTO, new GroupApplySendHandler());
        handlerMap.put(Message.SystemMessageRequestDTO, new SystemMessageHandler());
        handlerMap.put(Message.FriendApplyDealRequestDTO, new FriendApplyDealHandler());
        handlerMap.put(Message.GroupApplyDealRequestDTO, new GroupApplyDealHandler());
        handlerMap.put(Message.OnlineStatusListRequestDTO, new OnlineStatusListHandler());
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // 自定义的消息编码器会将消息转换为 Message 对象
        if (msg instanceof Message message) {
            log.info("收到消息类型: {}", message.getMessageType());
            // 根据消息类型，分发给对应的 Handler 处理
            MessageHandler handler = handlerMap.get(message.getMessageType());
            if (handler != null) {
                log.info("分发给 Handler: {}", handler.getClass().getSimpleName());
                MessageResult result = handler.handle(ctx, message);
                // 使用RabbitMQ发送消息到集群，而不是Redisson
                if (result != null) {
                    mqMessagePublish.sendToCluster(result);
                }
            } else {
                log.warn("未找到对应的 Handler 处理消息类型: {}", message.getMessageType());
            }
        }
        // 其他类型的消息，直接传递给下一个 Handler 处理，其实就是ctx.fireChannelRead(msg);
        super.channelRead(ctx, msg);
    }

}
