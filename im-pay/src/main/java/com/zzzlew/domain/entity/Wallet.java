package com.zzzlew.domain.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class Wallet {
    private Long id;
    private Long userId;
    private BigDecimal balance;
    private BigDecimal freezeBalance;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
