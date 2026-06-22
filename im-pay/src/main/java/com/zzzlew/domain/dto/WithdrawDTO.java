package com.zzzlew.domain.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class WithdrawDTO {
    /** 提现金额（元） */
    private BigDecimal amount;
    /** 提现备注 */
    private String remark;
}
