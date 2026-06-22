package com.zzzlew.domain.dto;

import lombok.Data;
import java.math.BigDecimal;

/** 供 im-chat Feign 调用：发红包扣款 */
@Data
public class DeductDTO {
    /** 扣款用户 ID */
    private Long userId;
    /** 扣款金额 */
    private BigDecimal amount;
    /** 业务 ID（红包 ID） */
    private Long businessId;
    /** 备注 */
    private String remark;
}
