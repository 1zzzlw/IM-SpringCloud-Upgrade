package com.zzzlew.handler.messageHandler;

import cn.hutool.extra.spring.SpringUtil;
import com.zzzlew.domain.request.OnlineStatusListRequestDTO;
import com.zzzlew.domain.response.OnlineStatusListResponseVO;
import com.zzzlew.handler.impl.MessageHandler;
import com.zzzlew.result.MessageResult;
import com.zzzlew.utils.ChannelManageUtil;
import io.netty.channel.ChannelHandlerContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import static com.zzzlew.constant.RedisConstant.USER_FRIEND_LIST_KEY;
import static com.zzzlew.constant.RedisConstant.USER_ONLINE_STATUS_KEY;

/**
 * 按消息类型分发的在线好友查询，结果直接回复当前连接，不进入 MQ。
 */
@Slf4j
public class OnlineStatusListHandler implements MessageHandler<OnlineStatusListRequestDTO> {

    private StringRedisTemplate stringRedisTemplate;

    @Override
    public MessageResult handle(ChannelHandlerContext ctx, OnlineStatusListRequestDTO request) {
        if (stringRedisTemplate == null) {
            stringRedisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
        }
        Long userId = ChannelManageUtil.getUser(ctx.channel()).getId();
        OnlineStatusListResponseVO snapshot = new OnlineStatusListResponseVO();
        snapshot.setFriendIdList(stringRedisTemplate.opsForSet()
                .intersect(USER_ONLINE_STATUS_KEY, USER_FRIEND_LIST_KEY + userId).stream().toList());
        ctx.writeAndFlush(snapshot).addListener(future -> {
            if (!future.isSuccess()) {
                log.warn("在线好友列表发送失败，用户: {}", userId, future.cause());
            }
        });
        // 分发器只将非空结果发送到 MQ；当前查询已直接回复。
        return null;
    }
}
