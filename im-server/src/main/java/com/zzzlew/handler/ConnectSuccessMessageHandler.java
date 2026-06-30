package com.zzzlew.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.zzzlew.constant.RedisConstant;
import com.zzzlew.domain.Message;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.response.LoginSuccessResponseVO;
import com.zzzlew.domain.response.OnlineStatusListResponseVO;
import com.zzzlew.publish.MQMessagePublish;
import com.zzzlew.result.MessageResult;
import com.zzzlew.utils.ChannelManageUtil;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.websocketx.WebSocketServerProtocolHandler;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.zzzlew.config.NettyConfig.NETTY_CLUSTER_ID;
import static com.zzzlew.constant.RedisConstant.*;


/**
 * @Auther: zzzlew
 * @Date: 2025/11/19 - 11 - 19 - 15:15
 * @Description: com.zzzlew.zzzimserver.websocket
 * @version: 1.0
 */
@Slf4j
@ChannelHandler.Sharable
public class ConnectSuccessMessageHandler extends ChannelInboundHandlerAdapter {

    private RedissonClient redissonClient;
    private StringRedisTemplate stringRedisTemplate;
    private MQMessagePublish mqMessagePublish;

    // 需要手动注入 StringRedisTemplate，不能使用 @Autowired 注解，因为 NettyWebSocketServerHandler 是手动创建的，
    // 而不是由 Spring 管理的，并且 handler不能注入到spring 容器中，所以需要手动注入 StringRedisTemplate
    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        if (this.redissonClient == null && this.stringRedisTemplate == null) {
            this.redissonClient = SpringUtil.getBean(RedissonClient.class);
            this.stringRedisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
            this.mqMessagePublish = SpringUtil.getBean(MQMessagePublish.class);
        }
    }

    /**
     * 通道就绪后调用，一般用来初始化
     *
     * @param ctx 通道处理器上下文（包含当前Channel信息）
     */
    @Override
    public void channelActive(ChannelHandlerContext ctx) {
        log.info("与客户端 {} 建立连接，通道开启！", ctx.channel().remoteAddress());
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) {
        if (evt instanceof WebSocketServerProtocolHandler.HandshakeComplete) {
            log.info("ws协议正式搭建完成，通道成功开启，客户端 {} 握手完成！", ctx.channel().remoteAddress());
            // 通知其他用户当前用户上线
            UserBaseDTO userInfo = ChannelManageUtil.getUser(ctx.channel());
            Long userId = userInfo.getId();

            if (hasOfflineMessages(userId)) {
                log.info("用户 {} 有离线消息，开始推送", userId);
                pushOfflineMessages(userId, ctx.channel());
            }

            String key = USER_FRIEND_LIST_KEY + userId;
            // 1 从redis中获取当前用户的好友列表id
            List<Long> friendIds = stringRedisTemplate.opsForSet().members(key).stream().map(s -> Long.valueOf(s)).toList();

            LoginSuccessResponseVO loginSuccessResponseVO = new LoginSuccessResponseVO();
            loginSuccessResponseVO.setUserId(userId);
            loginSuccessResponseVO.setAvatar(userInfo.getAvatar());
            loginSuccessResponseVO.setName(userInfo.getUsername());

            // 给好友发送自己上线的消息
            // RTopic topic = redissonClient.getTopic(SYSTEM_MESSAGE_BROADCAST);
            MessageResult result = MessageResult.multiple(loginSuccessResponseVO, friendIds);
            // topic.publish(result);
            mqMessagePublish.sendToCluster(result);

            String onlineStatusKey = RedisConstant.USER_ONLINE_STATUS_KEY;
            List<String> onlineFriends = stringRedisTemplate.opsForSet().intersect(onlineStatusKey, key).stream().toList();
            log.info("在线用户id集合: {}", onlineFriends);

            // 推送当前用户的好友在线状态集合
            OnlineStatusListResponseVO onlineStatusListResponseVO = new OnlineStatusListResponseVO();
            onlineStatusListResponseVO.setFriendIdList(onlineFriends);
            ctx.channel().writeAndFlush(onlineStatusListResponseVO);
            // 修改redis中的用户状态为在线
            stringRedisTemplate.opsForSet().add(onlineStatusKey, userId.toString());
            // 设置过期时间为270秒，因为用户在270秒内没有操作，就可以判断为用户不在线，对应心跳时间90秒
            stringRedisTemplate.expire(onlineStatusKey, RedisConstant.USER_ONLINE_STATUS_KEY_TTL, TimeUnit.SECONDS);

            // 将用户和Netty集群的对应关系存入redis中
            stringRedisTemplate.opsForHash().put(
                    USER_CLUSTER_MAPPING_KEY,
                    userId.toString(),
                    NETTY_CLUSTER_ID
            );
        }
    }

    /**
     * 推送离线消息给用户
     */
    public void pushOfflineMessages(Long userId, Channel channel) {
        String offlineMessageKey = USER_OFFLINE_MESSAGE_CONTENT_KEY + userId;

        // 使用Pipeline批量获取，减少网络开销
        List<Object> results = stringRedisTemplate.executePipelined((RedisCallback<Object>) connection -> {
            // 获取所有消息
            connection.zRange(offlineMessageKey.getBytes(), 0, -1);
            // 删除key
            connection.del(offlineMessageKey.getBytes());
            return null;
        });

        // stringRedisTemplate 会自动把 byte[] 转成 String，所以这里是 Set<String>
        @SuppressWarnings("unchecked") Set<String> messageJsonSet = (Set<String>) results.get(0);

        if (messageJsonSet == null || messageJsonSet.isEmpty()) {
            return;
        }

        // 推送消息
        int pushCount = 0;
        for (String messageJson : messageJsonSet) {
            // 检查 Channel 是否活跃
            if (!channel.isActive()) {
                log.warn("用户 {} 的 Channel 已断开，停止推送离线消息", userId);
                break;
            }

            try {
                Message message = parse(messageJson);
                channel.writeAndFlush(message);
                pushCount++;
            } catch (Exception e) {
                log.error("推送离线消息失败，messageJson: {}", messageJson, e);
            }
        }

        log.info("用户 {} 的 {} 条离线消息已推送", userId, pushCount);
    }

    /**
     * 检查是否有离线消息
     */
    public boolean hasOfflineMessages(Long userId) {
        String offlineMessageKey = USER_OFFLINE_MESSAGE_CONTENT_KEY + userId;
        Long count = stringRedisTemplate.opsForZSet().size(offlineMessageKey);
        return count != null && count > 0;
    }

    public Message parse(String json) {
        // 先解析成 JSONObject
        JSONObject jsonObject = JSON.parseObject(json);
        // 拿到 messageType
        int messageType = jsonObject.getIntValue("messageType");
        // 用已有的 messageClasses Map 拿到对应的 Class
        Class<? extends Message> clazz = Message.getMessageClass(messageType);
        if (clazz == null) {
            throw new RuntimeException("未知的 messageType: " + messageType);
        }
        // 4. 转换成对应的子类
        return jsonObject.toJavaObject(clazz);
    }
}
