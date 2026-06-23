package com.zzzlew.service;

import com.zzzlew.domain.entity.Wallet;
import com.zzzlew.domain.vo.WalletRecordVO;

import java.math.BigDecimal;
import java.util.List;

public interface WalletService {

    /**
     * 查询钱包
     */
    Wallet getWallet(Long userId);

    /**
     * 充值
     */
    void recharge(Long userId, BigDecimal amount, String remark);

    /**
     * 提现
     */
    void withdraw(Long userId, BigDecimal amount, String remark);

    /**
     * 扣款（供红包使用）
     */
    void deduct(Long userId, BigDecimal amount, Long businessId, String remark);

    /**
     * 退款（供红包补偿使用）
     */
    void refund(Long userId, BigDecimal amount, Long businessId, String remark);

    /**
     * 初始化钱包
     */
    void initWallet(Long userId);

    /**
     * 分页查询流水
     */
    List<WalletRecordVO> listRecords(Long userId, Integer type, int offset, int limit);

    /**
     * 查询流水总数
     */
    long countRecords(Long userId, Integer type);

    /**
     * 打赏转账：从 fromUserId 扣款，给 toUserId 加钱
     */
    void transferForReward(Long fromUserId, Long toUserId, BigDecimal amount, Long momentId);
}