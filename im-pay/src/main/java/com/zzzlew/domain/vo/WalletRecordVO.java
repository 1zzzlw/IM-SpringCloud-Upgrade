package com.zzzlew.domain.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WalletRecordVO {
    private Long id;
    private BigDecimal amount;
    /**
     * 1充值 2提现 3打赏支出 4打赏收入 5红包支出 6红包收入
     */
    private Integer type;
    private String typeDesc;
    private Long businessId;
    private BigDecimal beforeBalance;
    private BigDecimal afterBalance;
    private String remark;
    private LocalDateTime createTime;
}
