package com.zzzlew.domain.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 打赏结果 MQ 消息体（im-pay -> im-moments）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RewardResultDTO implements Serializable {

    /** 与请求一致的幂等 ID */
    private String idempotentKey;

    /** 帖子 ID */
    private Long momentId;

    /** 打赏金额 */
    private BigDecimal amount;

    /** true=成功，false=失败 */
    private boolean success;

    /** 失败原因（成功时为 null） */
    private String failReason;
}
