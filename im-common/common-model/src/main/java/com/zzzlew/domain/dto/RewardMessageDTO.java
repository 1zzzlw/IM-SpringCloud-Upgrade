package com.zzzlew.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 打赏 MQ 消息体
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardMessageDTO implements Serializable {

    /** 幂等 ID，防重复消费 */
    private String idempotentKey;

    /** 帖子 ID */
    private Long momentId;

    /** 打赏人 ID */
    private Long fromUserId;

    /** 帖子作者（收款人）ID */
    private Long toUserId;

    /** 打赏金额 */
    private BigDecimal amount;
}
