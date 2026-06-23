package com.zzzlew.service.impl;

import com.zzzlew.domain.entity.Wallet;
import com.zzzlew.domain.vo.WalletRecordVO;
import com.zzzlew.mapper.WalletMapper;
import com.zzzlew.service.WalletService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Service
public class WalletServiceImpl implements WalletService {

    @Resource
    private WalletMapper walletMapper;

    @Override
    public Wallet getWallet(Long userId) {
        Wallet wallet = walletMapper.selectByUserId(userId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在，userId: " + userId);
        }
        return wallet;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void recharge(Long userId, BigDecimal amount, String remark) {
        if (amount == null || amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new RuntimeException("充值金额不合法");
        }
        Wallet wallet = walletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在，userId: " + userId);
        }
        int rows = walletMapper.addBalance(userId, amount);
        if (rows == 0) throw new RuntimeException("充值失败");
        BigDecimal after = wallet.getBalance().add(amount);
        walletMapper.insertRecord(userId, amount, 1, null, wallet.getBalance(), after,
                remark != null ? remark : "充值");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdraw(Long userId, BigDecimal amount, String remark) {
        if (amount == null || amount.compareTo(new BigDecimal("0.01")) < 0) {
            throw new RuntimeException("提现金额不合法");
        }
        Wallet wallet = walletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null || wallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足");
        }
        int rows = walletMapper.deductBalance(userId, amount);
        if (rows == 0) throw new RuntimeException("余额不足或并发冲突");
        BigDecimal after = wallet.getBalance().subtract(amount);
        walletMapper.insertRecord(userId, amount.negate(), 2, null, wallet.getBalance(), after,
                remark != null ? remark : "提现");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deduct(Long userId, BigDecimal amount, Long businessId, String remark) {
        Wallet wallet = walletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) throw new RuntimeException("钱包不存在");
        if (wallet.getBalance().compareTo(amount) < 0) throw new RuntimeException("余额不足");
        int rows = walletMapper.deductBalance(userId, amount);
        if (rows == 0) throw new RuntimeException("扣款失败，余额不足或并发冲突");
        BigDecimal after = wallet.getBalance().subtract(amount);
        walletMapper.insertRecord(userId, amount.negate(), 5, businessId, wallet.getBalance(), after,
                remark != null ? remark : "发送红包");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, BigDecimal amount, Long businessId, String remark) {
        Wallet wallet = walletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) {
            walletMapper.initWallet(userId);
            wallet = walletMapper.selectByUserIdForUpdate(userId);
        }
        walletMapper.addBalance(userId, amount);
        BigDecimal after = wallet.getBalance().add(amount);
        walletMapper.insertRecord(userId, amount, 7, businessId, wallet.getBalance(), after,
                remark != null ? remark : "退款");
    }

    @Override
    public void initWallet(Long userId) {
        walletMapper.initWallet(userId);
    }

    @Override
    public List<WalletRecordVO> listRecords(Long userId, Integer type, int offset, int limit) {
        return walletMapper.selectRecords(userId, type, offset, limit);
    }

    @Override
    public long countRecords(Long userId, Integer type) {
        return walletMapper.countRecords(userId, type);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void transferForReward(Long fromUserId, Long toUserId, BigDecimal amount, Long momentId) {
        log.info("开始打赏转账：from={}, to={}, amount={}, momentId={}", fromUserId, toUserId, amount, momentId);

        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("打赏金额必须大于0");
        }

        // 按 userId 升序锁定，避免两个方向同时转账时产生死锁
        long firstId = Math.min(fromUserId, toUserId);
        long secondId = Math.max(fromUserId, toUserId);

        Wallet firstWallet = walletMapper.selectByUserIdForUpdate(firstId);
        if (firstWallet == null) {
            throw new RuntimeException("钱包不存在，userId: " + firstId);
        }
        // 收款人钱包不存在则自动初始化
        if (secondId != firstId) {
            walletMapper.initWallet(secondId);
        }
        Wallet secondWallet = walletMapper.selectByUserIdForUpdate(secondId);
        if (secondWallet == null) {
            throw new RuntimeException("钱包初始化失败，userId: " + secondId);
        }

        // 确定哪个是从/哪个是到
        Wallet fromWallet = firstId == fromUserId ? firstWallet : secondWallet;
        Wallet toWallet = firstId == fromUserId ? secondWallet : firstWallet;

        if (fromWallet.getBalance().compareTo(amount) < 0) {
            throw new RuntimeException("余额不足，当前余额: " + fromWallet.getBalance() + "，需要: " + amount);
        }

        // 扣款（乐观锁）
        int deductResult = walletMapper.deductBalance(fromUserId, amount);
        if (deductResult == 0) {
            throw new RuntimeException("扣款失败，余额不足或并发冲突");
        }

        // 到账
        int addResult = walletMapper.addBalance(toUserId, amount);
        if (addResult == 0) {
            throw new RuntimeException("到账失败");
        }

        // 记录流水
        BigDecimal fromAfter = fromWallet.getBalance().subtract(amount);
        BigDecimal toAfter = toWallet.getBalance().add(amount);

        walletMapper.insertRecord(fromUserId, amount.negate(), 3, momentId,
                fromWallet.getBalance(), fromAfter, "打赏帖子" + momentId);
        walletMapper.insertRecord(toUserId, amount, 4, momentId,
                toWallet.getBalance(), toAfter, "收到打赏，帖子" + momentId);

        log.info("打赏转账成功：from={} -> to={}, amount={}", fromUserId, toUserId, amount);
    }
}