package com.zzzlew.listener;

import com.zzzlew.domain.Message;
import com.zzzlew.result.MessageResult;
import com.zzzlew.utils.ChannelManageUtil;
import io.netty.channel.Channel;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RTopic;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.List;

import static com.zzzlew.constant.RedisConstant.SYSTEM_MESSAGE_BROADCAST;

/**
 * @Auther: zzzlew
 * @Date: 2026/2/8 - 02 - 08 - 22:50
 * @Description: com.zzzlew.listener
 * @version: 1.0
 */
@Slf4j
@Configuration
public class BroadCastListener {

    @Resource
    private RedissonClient redissonClient;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @PostConstruct
    public void initBroadCastListener() {
        RTopic topic = redissonClient.getTopic(SYSTEM_MESSAGE_BROADCAST);
        topic.addListener(MessageResult.class, (channel, result) -> {
            Message response = result.getResponse();
            List<Long> receiverIds = result.getReceiverIds();

            // 只发送给本地的接收者
            for (Long receiverId : receiverIds) {
                for (Channel receiverChannel : ChannelManageUtil.getChannels(receiverId)) {
                    if (ChannelManageUtil.isCurrent(receiverChannel, stringRedisTemplate)) {
                        receiverChannel.writeAndFlush(response);
                    }
                }
            }
        });
    }

}
