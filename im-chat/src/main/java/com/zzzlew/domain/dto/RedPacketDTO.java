package com.zzzlew.domain.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 发送红包请求DTO
 */
@Data
public class RedPacketDTO {

    /**
     * 关联消息ID（前端预生成雪花ID）
     */
    private Long messageId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 接收者ID（私聊时为对方ID，群聊时为群ID）
     */
    private String receiverId;

    /**
     * 红包总金额（元）
     */
    private BigDecimal totalAmount;

    /**
     * 红包个数
     */
    private Integer totalCount;

    /**
     * 红包类型：0-拼手气，1-普通（均分）
     */
    private Integer type;

    /**
     * 祝福语
     */
    private String greeting;

    /**
     * 接收者ID列表（群聊时为群成员ID列表）
     */
    private java.util.List<Long> receiverIds;
}
