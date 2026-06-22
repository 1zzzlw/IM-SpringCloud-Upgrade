package com.zzzlew.domain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class RechargeDTO {
    /** 充值金额（元） */
    private BigDecimal amount;
    /** 备注 */
    private String remark;
}
