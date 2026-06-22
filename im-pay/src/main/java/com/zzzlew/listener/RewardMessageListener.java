package com.zzzlew.listener;

import com.alibaba.fastjson.JSON;
import com.rabbitmq.client.Channel;
import com.zzzlew.domain.dto.RewardMessageDTO;
import com.zzzlew.domain.dto.RewardResultDTO;
import com.zzzlew.lock.DistributedLockUtil;
import com.zzzlew.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;

import static com.zzzlew.constant.RabbitMQConstant.EXCHANGE;
import static com.zzzlew.constant.RabbitMQConstant.ROUTING_KEY_REWARD_RESULT;

@Slf4j
@Component
public class RewardMessageListener {

    @Resource
    private WalletService walletService;
    @Resource
    private RabbitTemplate rabbitTemplate;
    @Resource
    private DistributedLockUtil lockUtil;

    @RabbitListener(queues = "im-reward-create-queue")
    public void handleRewardRequest(RewardMessageDTO rewardMsg, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        String idempotentKey = rewardMsg.getIdempotentKey();
        log.info("收到打赏请求：{}", JSON.toJSONString(rewardMsg));

        try {
            // Redisson 幂等：TTL=300s，同一 key 只执行一次
            boolean executed = lockUtil.executeIfAbsent("reward:" + idempotentKey, 300, () -> {
                walletService.transferForReward(
                        rewardMsg.getFromUserId(),
                        rewardMsg.getToUserId(),
                        rewardMsg.getAmount(),
                        rewardMsg.getMomentId()
                );
                RewardResultDTO result = new RewardResultDTO(
                        idempotentKey, rewardMsg.getMomentId(), rewardMsg.getAmount(), true, null);
                rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_REWARD_RESULT, result);
                log.info("打赏处理成功：{}", idempotentKey);
            });

            if (!executed) {
                log.warn("打赏重复消费，已跳过：{}", idempotentKey);
            }
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("打赏处理失败：{}", idempotentKey, e);
            try {
                RewardResultDTO result = new RewardResultDTO(
                        idempotentKey, rewardMsg.getMomentId(), rewardMsg.getAmount(), false, e.getMessage());
                rabbitTemplate.convertAndSend(EXCHANGE, ROUTING_KEY_REWARD_RESULT, result);
                channel.basicAck(deliveryTag, false);
            } catch (Exception ex) {
                log.error("发送打赏失败结果异常：{}", idempotentKey, ex);
                try {
                    channel.basicNack(deliveryTag, false, true);
                } catch (Exception ignore) {
                }
            }
        }
    }
}
