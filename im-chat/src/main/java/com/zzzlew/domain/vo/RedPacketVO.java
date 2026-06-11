package com.zzzlew.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 红包详情VO（查看红包详情时使用）
 */
@Data
public class RedPacketVO {

    /** 红包ID */
    private Long id;

    /** 发送者ID */
    private Long senderId;

    /** 发送者名称 */
    private String senderName;

    /** 红包总金额（元） */
    private BigDecimal totalAmount;

    /** 红包总个数 */
    private Integer totalCount;

    /** 剩余金额（元） */
    private BigDecimal remainAmount;

    /** 剩余个数 */
    private Integer remainCount;

    /** 红包类型：0-拼手气，1-普通 */
    private Integer type;

    /** 祝福语 */
    private String greeting;

    /** 状态：0-进行中，1-已领完，2-已过期 */
    private Integer status;

    /** 当前用户是否已领取 */
    private Boolean grabbed;

    /** 当前用户领取的金额 */
    private BigDecimal grabbedAmount;

    /** 领取记录列表（查看详情时返回） */
    private List<GrabRecordVO> records;

    /** 领取时间 */
    private LocalDateTime createdAt;

    /**
     * 领取记录内部类
     */
    @Data
    public static class GrabRecordVO {
        private Long userId;
        private String username;
        private String avatar;
        private BigDecimal amount;
        private LocalDateTime createdAt;
    }
}
