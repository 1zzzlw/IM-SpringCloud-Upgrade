package com.zzzlew.domain.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 红包实体
 */
@Data
public class RedPacket {

    /** 红包ID */
    private Long id;

    /** 关联消息ID */
    private Long messageId;

    /** 会话ID */
    private String conversationId;

    /** 发送者ID */
    private Long senderId;

    /** 单聊接收人ID */
    private String receiverId;

    /** 聊天类型：1单聊 2群聊 */
    private Integer chatType;

    /** 红包总金额（元） */
    private BigDecimal totalAmount;

    /** 红包总个数 */
    private Integer totalCount;

    /** 剩余金额（元） */
    private BigDecimal remainAmount;

    /** 剩余个数 */
    private Integer remainCount;

    /** 红包类型：0-拼手气，1-普通（均分） */
    private Integer type;

    /** 祝福语 */
    private String greeting;

    /** 状态：0-进行中，1-已领完，2-已过期，3-已撤回 */
    private Integer status;

    /** 过期时间 */
    private LocalDateTime expireTime;

    /** 创建时间 */
    private LocalDateTime createdAt;
}
