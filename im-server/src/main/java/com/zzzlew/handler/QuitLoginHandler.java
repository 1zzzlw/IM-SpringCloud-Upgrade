package com.zzzlew.handler;

import cn.hutool.extra.spring.SpringUtil;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.response.QuitLoginResponseVO;
import com.zzzlew.publish.MQMessagePublish;
import com.zzzlew.result.MessageResult;
import com.zzzlew.utils.ChannelManageUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RedissonClient;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import static com.zzzlew.constant.RedisConstant.*;


/**
 * @Auther: zzzlew
 * @Date: 2025/11/19 - 11 - 19 - 16:23
 * @Description: com.zzzlew.zzzimserver.handler
 * @version: 1.0
 */
@Slf4j
@ChannelHandler.Sharable
public class QuitLoginHandler extends ChannelInboundHandlerAdapter {

    private RedissonClient redissonClient;
    private StringRedisTemplate stringRedisTemplate;
    private MQMessagePublish mqMessagePublish;

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) {
        if (this.redissonClient == null && this.stringRedisTemplate == null) {
            this.redissonClient = SpringUtil.getBean(RedissonClient.class);
            this.stringRedisTemplate = SpringUtil.getBean(StringRedisTemplate.class);
            this.mqMessagePublish = SpringUtil.getBean(MQMessagePublish.class);
        }
    }

    /**
     * 当Channel处理过程中发生异常时触发（如IO异常、消息解析失败等）。 此处采用简单策略：关闭异常通道，避免异常扩散影响其他连接。
     *
     * @param ctx   通道处理器上下文
     * @param cause 异常原因
     * @throws Exception 处理过程中的异常
     */
    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        // 异常关闭通道
        log.error("发生异常，通道关闭！", cause);
        ctx.channel().close();
    }

    /**
     * 当Handler从Channel的Pipeline中移除时触发（通常在连接断开后）。 主要用于清理当前Handler与Channel的绑定关系，补充处理用户离线逻辑。
     *
     * @param ctx 通道处理器上下文（包含当前Channel信息）
     * @throws Exception 处理过程中的异常
     */
    @Override
    public void handlerRemoved(ChannelHandlerContext ctx) throws Exception {
        log.info("Handler从Channel的Pipeline中移除，通道关闭！");
        offLine(ctx);
    }

    /**
     * 处理用户离线逻辑的私有方法，统一封装离线时的资源清理、状态更新操作。 调用webSocketService.offline()更新用户在线状态、移除Channel映射等。
     *
     * @param ctx 通道处理器上下文
     */
    private void offLine(ChannelHandlerContext ctx) {
        ctx.channel().close();
    }

    /**
     * 当TCP连接断开（Channel从活跃状态变为非活跃状态）时触发。 触发场景包括：客户端主动关闭连接、网络中断、服务端主动关闭等。 主要用于最终确认连接断开，并执行离线清理。
     *
     * @param ctx 通道处理器上下文
     */
    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        // 无论客户端何种方式断开连接，都会执行这里的离线逻辑
        log.info("与客户端 {} 断开连接，通道关闭！", ctx.channel().remoteAddress());
        // 推送离线消息给他的好友
        // 获得用户的基础信息
        UserBaseDTO userBaseDTO = ChannelManageUtil.getUser(ctx.channel());
        log.info("用户 {} 离线！", userBaseDTO.getId());
        String key = USER_FRIEND_LIST_KEY + userBaseDTO.getId();
        // 1 从redis中获取当前用户的好友列表id
        List<Long> friendIds =
                stringRedisTemplate.opsForSet().members(key).stream().map(s -> Long.valueOf(s)).toList();

        QuitLoginResponseVO quitLoginResponseVO = new QuitLoginResponseVO();
        quitLoginResponseVO.setUserId(userBaseDTO.getId());

        // 推送退出的消息给好友，即使更新在线状态
        MessageResult result = MessageResult.multiple(quitLoginResponseVO, friendIds);
        mqMessagePublish.sendToCluster(result);
        // RTopic topic = redissonClient.getTopic(SYSTEM_MESSAGE_BROADCAST);
        // topic.publish(result);

        // TODO 将离线时间存储在redis中，因为有20s的时间可以推送离线消息，因此，再用户正常退出登录的时候拉取离线消息的时间可以晚20s
        LocalDateTime offlineTime = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        String offlineTimeStr = offlineTime.format(formatter);
        String userId = userBaseDTO.getId().toString();
        stringRedisTemplate.opsForHash().put(USER_OFFLINE_INFO_KEY, userId,
                offlineTimeStr);
        // 移除该用户的在线状态
        stringRedisTemplate.opsForSet().remove(USER_ONLINE_STATUS_KEY, userId);
        ChannelManageUtil.removeChannel(ctx.channel());

        // 用户断开连接时，移除用户和Netty集群的对应关系
        stringRedisTemplate.opsForHash().delete(USER_CLUSTER_MAPPING_KEY, userId);

        offLine(ctx);
    }

}
