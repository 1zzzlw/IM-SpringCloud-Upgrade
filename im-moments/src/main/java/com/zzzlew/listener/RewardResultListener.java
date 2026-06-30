package com.zzzlew.listener;

import com.rabbitmq.client.Channel;
import com.zzzlew.domain.dto.RewardResultDTO;
import com.zzzlew.mapper.MomentsMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import java.util.concurrent.TimeUnit;

/**
 * 消费 im-pay 发回的打赏结果，成功则更新帖子累计打赏金额
 */
@Slf4j
@Component
public class RewardResultListener {

    @Resource
    private MomentsMapper momentsMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String REWARD_RESULT_IDEMPOTENT_PREFIX = "reward:result:";

    @RabbitListener(queues = "im-reward-result-queue")
    public void handleRewardResult(RewardResultDTO result, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String idempotentKey = result.getIdempotentKey();
        log.info("收到打赏结果：idempotentKey={}, success={}, momentId={}", idempotentKey, result.isSuccess(), result.getMomentId());

        try {
            if (result.isSuccess()) {
                // 幂等检查：同一结果只处理一次
                String redisKey = REWARD_RESULT_IDEMPOTENT_PREFIX + idempotentKey;
                Boolean firstTime = stringRedisTemplate.opsForValue().setIfAbsent(redisKey, "1", 24, TimeUnit.HOURS);
                if (Boolean.TRUE.equals(firstTime)) {
                    momentsMapper.updateRewardAmount(result.getMomentId(), result.getAmount());
                    log.info("帖子 {} 打赏金额已更新 +{}", result.getMomentId(), result.getAmount());
                } else {
                    log.warn("打赏结果重复消费，已跳过：idempotentKey={}", idempotentKey);
                }
            } else {
                log.warn("打赏失败，momentId={}, reason={}", result.getMomentId(), result.getFailReason());
            }
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("处理打赏结果异常，momentId={}", result.getMomentId(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) {
                log.error("NACK 失败", ex);
            }
        }
    }
}
