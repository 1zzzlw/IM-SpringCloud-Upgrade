package com.zzzlew.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.zzzlew.domain.Message;
import com.zzzlew.domain.request.HeartRequestDTO;
import com.zzzlew.utils.ChannelManageUtil;
import io.netty.channel.ChannelDuplexHandler;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.concurrent.TimeUnit;

import static com.zzzlew.constant.RedisConstant.*;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/19 - 11 - 19 - 14:22
 * @Description: com.zzzlew.zzzimserver.websocket
 * @version: 1.0
 */
@Slf4j
@ChannelHandler.Sharable
// 统一处理应用层心跳、缓存续期以及读空闲超时。
public class HeartBeatHandler extends ChannelDuplexHandler {

    private StringRedisTemplate stringRedisTemplate;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        if (stringRedisTemplate == null) {
            stringRedisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
        }
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof Message message && message.getMessageType() == Message.HeartRequestDTO) {
            Long userId = ChannelManageUtil.getUser(ctx.channel()).getId();
            renewHeartbeatCache(userId);
            HeartRequestDTO pong = new HeartRequestDTO();
            pong.setSequenceId(message.getSequenceId());
            ctx.writeAndFlush(pong);
            return;
        }
        ctx.fireChannelRead(msg);
    }

    private void renewHeartbeatCache(Long userId) {
        log.debug("接收心跳消息，更新redis缓存");
        stringRedisTemplate.expire(USER_FRIEND_LIST_KEY + userId, USER_FRIEND_LIST_KEY_TTL, TimeUnit.MINUTES);
        stringRedisTemplate.expire(USER_ONLINE_STATUS_KEY, USER_ONLINE_STATUS_KEY_TTL, TimeUnit.SECONDS);
    }

    // 处理非IO事件的入口，事件会通过userEventTriggered方法传递给处理器
    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent e) {
            // IdleStateHandler 产生的读空闲事件，并非客户端发送的心跳消息。
            if (e.state() == IdleState.READER_IDLE) {
                // 处理读空闲事件
                log.info("心跳超时（90秒未读），关闭连接：{}", ctx.channel().remoteAddress());
                ctx.close();
                return;
            }
        }
        // 其他事件，继续传递
        ctx.fireUserEventTriggered(evt);
    }

}
