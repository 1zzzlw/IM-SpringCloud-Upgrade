package com.zzzlew.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 红包领取记录实体
 */
@Data
public class RedPacketRecord {

    /** 记录ID */
    private Long id;

    /** 红包ID */
    private Long redPacketId;

    /** 领取用户ID */
    private Long userId;

    /** 领取金额（元） */
    private BigDecimal amount;

    /** 领取时间 */
    private LocalDateTime createdAt;
}
