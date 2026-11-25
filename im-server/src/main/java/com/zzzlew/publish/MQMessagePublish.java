package com.zzzlew.publish;

import com.alibaba.fastjson.JSON;
import com.zzzlew.config.NettyConfig;
import com.zzzlew.domain.ClusterMessageWrapper;
import com.zzzlew.domain.Message;
import com.zzzlew.domain.response.GroupChatResponseVO;
import com.zzzlew.domain.response.PrivateChatResponseVO;
import com.zzzlew.domain.response.SystemMessageResponseVO;
import com.zzzlew.result.MessageResult;
import com.zzzlew.utils.ChannelManageUtil;
import com.zzzlew.utils.DevicePresence;
import io.netty.channel.Channel;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static com.zzzlew.constant.RabbitMQConstant.*;
import static com.zzzlew.constant.RedisConstant.*;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/24 - 03 - 24 - 16:07
 * @Description: com.zzzlew.publish
 * @version: 1.0
 */
@Slf4j
@Component
public class MQMessagePublish {

    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource(name = "imAsyncExecutor")
    private ExecutorService imAsyncExecutor;

    private static final DefaultRedisScript<Long> storeOfflineMessageScript;

    static {
        // 提前读取lua文件，提高效率
        storeOfflineMessageScript = new DefaultRedisScript<>();
        // 设置lua脚本文件的位置
        storeOfflineMessageScript.setLocation(new ClassPathResource("lua/store_offline_message.lua"));
        // 指定返回值
        storeOfflineMessageScript.setResultType(Long.class);
    }

    /**
     * 发送消息到集群
     * 智能路由：只发送到接收者所在的集群队列
     */
    // public void sendToCluster(MessageResult messageResult) {
    //     // 其中包含单聊和群聊的id集合
    //     List<Long> receiverIds = messageResult.getReceiverIds();
    //
    //     for (Long receiverId : receiverIds) {
    //         String nettyClusterKey = USER_CLUSTER_MAPPING_KEY + receiverId;
    //         // 获取接收者所在的集群ID
    //         String serverId = stringRedisTemplate.opsForValue().get(nettyClusterKey);
    //
    //         if (!StringUtils.isBlank(serverId)) {
    //             // 构建路由键
    //             String routingKey = QUEUE_NETTY_ROUTING_KEY + serverId;
    //             // 发送消息到指定Netty集群的消息队列
    //             rabbitTemplate.convertAndSend(EXCHANGE, routingKey, new ClusterMessageWrapper<Message>(messageResult.getResponse(), receiverId));
    //             log.info("消息已发送到集群: {} 接收者: {}", serverId, receiverId);
    //         } else {
    //             // 用户不在线或未找到集群信息
    //             log.info("用户: {} 不在线或未找到集群信息", receiverId);
    //             // 用户离线，记录离线消息标记到Redis
    //             recordOfflineMessageMarker(receiverId, messageResult.getResponse());
    //         }
    //     }
    //
    //     if (messageResult.getResponse() instanceof PrivateChatResponseVO || messageResult.getResponse() instanceof GroupChatResponseVO) {
    //         // 将消息发送到SpringBoot监听的消息队列中用来异步存储，如果用户不在线还可以直接从这里拉取离线消息
    //         rabbitTemplate.convertAndSend(EXCHANGE, QUEUE_STORGE_ROUTING_KEY, new ClusterMessageWrapper<Message>(messageResult.getResponse()));
    //     }
    // }

    /**
     * 发送消息到集群（优化版：本地直接发送，远程走MQ）
     */
    public void sendToCluster(MessageResult messageResult) {
        sendToCluster(messageResult, null);
    }

    public void sendToCluster(MessageResult messageResult, Channel source) {
        Message message = messageResult.getResponse();
        boolean chat = message instanceof PrivateChatResponseVO || message instanceof GroupChatResponseVO;
        boolean persist = chat || message instanceof SystemMessageResponseVO;
        Long sender = source != null && chat ? ChannelManageUtil.getUser(source).getId() : null;
        String sourceConnection = source != null && chat ? source.id().asLongText() : null;

        var recipients = new LinkedHashSet<Long>();
        if (messageResult.getReceiverIds() != null) recipients.addAll(messageResult.getReceiverIds());
        if (sender != null) recipients.add(sender);
        if (recipients.isEmpty()) return;
        List<Long> receiverIds = List.copyOf(recipients);

        CompletableFuture.runAsync(() -> {
            try {
                List<Object> userIds = receiverIds.stream().map(id -> (Object) id.toString()).toList();
                List<Object> routes = stringRedisTemplate.opsForHash().multiGet(USER_CLUSTER_MAPPING_KEY, userIds);
                for (int i = 0; i < receiverIds.size(); i++) {
                    Long receiver = receiverIds.get(i);
                    Object route = routes == null || i >= routes.size() ? null : routes.get(i);
                    var nodes = DevicePresence.groupByNode(route, sourceConnection);
                    boolean scheduled = false;
                    for (var node : nodes.entrySet()) {
                        if (NettyConfig.NETTY_CLUSTER_ID.equals(node.getKey())) {
                            scheduled |= deliverLocally(receiver, message, node.getValue());
                        } else {
                            sendToNode(node.getKey(), receiver, message, node.getValue());
                            scheduled = true;
                        }
                    }
                    if (!scheduled && persist && !receiver.equals(sender)) {
                        recordOfflineMessageMarker(receiver, message);
                    }
                }
                if (persist) {
                    rabbitTemplate.convertAndSend(EXCHANGE, QUEUE_STORGE_ROUTING_KEY,
                            new ClusterMessageWrapper<>(message));
                }
            } catch (Exception e) {
                log.error("异步投递集群消息时发生异常", e);
            }
        }, imAsyncExecutor);
    }

    private boolean deliverLocally(Long userId, Message message, List<String> connections) {
        boolean scheduled = false;
        boolean presence = message.getMessageType() == Message.LoginSuccessResponseVO ||
                message.getMessageType() == Message.QuitLoginResponseVO;
        for (Channel channel : ChannelManageUtil.getChannels(userId)) {
            String connection = channel.id().asLongText();
            if (!connections.contains(connection)) continue;
            if (message.getMessageType() == Message.DevicePresenceResponseVO) {
                DevicePresence.pushSnapshot(stringRedisTemplate, channel);
                scheduled = true;
                continue;
            }
            if (!ChannelManageUtil.isCurrent(channel, stringRedisTemplate)) continue;
            if (!channel.isWritable() && !presence) continue;
            scheduled = true;
            channel.writeAndFlush(message).addListener(future -> {
                if (!future.isSuccess()) {
                    sendToNode(NettyConfig.NETTY_CLUSTER_ID, userId, message, List.of(connection));
                }
            });
        }
        return scheduled;
    }

    private void sendToNode(String node, Long userId, Message message, List<String> connections) {
        rabbitTemplate.convertAndSend(EXCHANGE, QUEUE_NETTY_ROUTING_KEY + node,
                new ClusterMessageWrapper<>(message, userId, connections));
    }

    public void recordOfflineMessageMarker(Long userId, Message message) {
        String offlineMessageKey = USER_OFFLINE_MESSAGE_CONTENT_KEY + userId;
        String messageJson = JSON.toJSONString(message);

        // 使用消息雪花ID作为score（而非wall-clock时间），保证离线消息按发送顺序投递
        long score = extractMessageSnowflakeId(message);

        // 执行Lua脚本
        Long count = stringRedisTemplate.execute(storeOfflineMessageScript, Collections.singletonList(offlineMessageKey), messageJson, String.valueOf(score), String.valueOf(USER_OFFLINE_MESSAGE_KEY_TTL));

        log.debug("用户 {} 离线消息已存储，当前消息数: {}", userId, count);
    }

    /**
     * 从消息响应中提取雪花ID作为排序score
     */
    private long extractMessageSnowflakeId(Message message) {
        String id = null;
        if (message instanceof PrivateChatResponseVO) {
            id = ((PrivateChatResponseVO) message).getId();
        } else if (message instanceof GroupChatResponseVO) {
            id = ((GroupChatResponseVO) message).getId();
        } else if (message instanceof SystemMessageResponseVO) {
            id = ((SystemMessageResponseVO) message).getId();
        }
        if (id != null && !id.isEmpty()) {
            try {
                return Long.parseLong(id);
            } catch (NumberFormatException e) {
                log.warn("消息ID无法解析为long: {}, 回退为当前时间戳", id);
            }
        }
        return System.currentTimeMillis();
    }

    /**
     * 广播消息到所有集群
     */
    public void broadcastToAllClusters(MessageResult messageResult) {
    }

}
