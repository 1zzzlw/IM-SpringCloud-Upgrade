package com.zzzlew.listener;

import com.rabbitmq.client.Channel;
import com.zzzlew.domain.entity.Wallet;
import com.zzzlew.mapper.WalletMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 消费红包领取事件：给领取者到账 + 写流水
 */
@Slf4j
@Component
public class RedPacketGrabListener {

    @Resource
    private WalletMapper walletMapper;

    @Transactional(rollbackFor = Exception.class)
    @RabbitListener(queues = "im-redpacket-grab-queue")
    public void handleGrab(Map<String, Object> event, Message message, Channel channel) {
        long deliveryTag = message.getMessageProperties().getDeliveryTag();
        try {
            Long userId = Long.valueOf(event.get("userId").toString());
            Long redPacketId = Long.valueOf(event.get("redPacketId").toString());
            BigDecimal amount = new BigDecimal(event.get("amount").toString());

            log.info("红包到账：userId={}, redPacketId={}, amount={}", userId, redPacketId, amount);

            // 查钱包（加锁）
            Wallet wallet = walletMapper.selectByUserIdForUpdate(userId);
            if (wallet == null) {
                log.error("用户 {} 钱包不存在，红包到账失败", userId);
                channel.basicAck(deliveryTag, false);
                return;
            }

            // 到账
            walletMapper.addBalance(userId, amount);
            BigDecimal after = wallet.getBalance().add(amount);

            // 写流水 type=6 红包收入
            walletMapper.insertRecord(userId, amount, 6, redPacketId,
                    wallet.getBalance(), after, "领取红包");

            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("红包到账处理失败", e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (Exception ex) { /* ignore */ }
        }
    }
}
