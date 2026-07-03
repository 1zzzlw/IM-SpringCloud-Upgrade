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
        // 锁一：SELECT … FOR UPDATE 悲观锁串行化
        Wallet wallet = walletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) throw new RuntimeException("钱包不存在");
        if (wallet.getBalance().compareTo(amount) < 0) throw new RuntimeException("余额不足");
        // 锁二：WHERE balance >= amount 乐观锁二次拦截
        int rows = walletMapper.deductBalance(userId, amount);
        if (rows == 0) throw new RuntimeException("扣款失败，余额不足或并发冲突");
        BigDecimal after = wallet.getBalance().subtract(amount);
        walletMapper.insertRecord(userId, amount.negate(), 5, businessId, wallet.getBalance(), after,
                remark != null ? remark : "发送红包");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void refund(Long userId, BigDecimal amount, Long businessId, String remark) {
        // 锁一：SELECT … FOR UPDATE 悲观锁串行化
        Wallet wallet = walletMapper.selectByUserIdForUpdate(userId);
        if (wallet == null) {
            throw new RuntimeException("钱包不存在，userId: " + userId);
        }
        walletMapper.addBalance(userId, amount);
        BigDecimal after = wallet.getBalance().add(amount);
        walletMapper.insertRecord(userId, amount, 7, businessId, wallet.getBalance(), after,
                remark != null ? remark : "退款");
    }

    /**
     * 初始化用户钱包的方法
     *
     * @param userId 用户ID，用于标识需要初始化钱包的用户
     */
    @Override
    public void initWallet(Long userId) {
        // 调用walletMapper的initWallet方法执行数据库层面的钱包初始化操作
        walletMapper.initWallet(userId);
    }


    /**
     * 查询指定用户的钱包记录列表
     *
     * @param userId 用户ID，用于筛选特定用户的记录
     * @param type   记录类型，用于筛选特定类型的记录
     * @param offset 分页偏移量，用于分页查询
     * @param limit  每页记录数，用于分页查询
     * @return 返回符合条件钱包记录的列表
     */
    @Override
    public List<WalletRecordVO> listRecords(Long userId, Integer type, int offset, int limit) {
        // 调用数据访问层方法查询钱包记录
        return walletMapper.selectRecords(userId, type, offset, limit);
    }


    /**
     * 重写父类方法，用于统计指定用户和类型的记录数量
     *
     * @param userId 用户ID，用于标识特定用户
     * @param type   记录类型，用于筛选特定类型的记录
     * @return 返回符合条件的记录总数
     */
    @Override
    public long countRecords(Long userId, Integer type) {
        // 调用walletMapper的countRecords方法查询记录数量
        return walletMapper.countRecords(userId, type);
    }


    /**
     * 打赏转账方法：该方法采用三层防护，分别是userId排序防止双方相互转账导致的死锁，以及数据库方面的乐观锁和悲观锁，最后是事务的回滚机制，确保转账操作的原子性。
     * 保证了多个请求抵达时，能够保证多个请求修改同一条数据的情况下的线程安全问题
     *
     * @param fromUserId 转出用户ID
     * @param toUserId   接收用户ID
     * @param amount     转账金额
     * @param momentId   动态/帖子ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class) // 使用事务注解，确保方法内所有数据库操作要么全部成功，要么全部回滚，并设置回滚条件为所有异常
    public void transferForReward(Long fromUserId, Long toUserId, BigDecimal amount, Long momentId) {
        log.info("开始打赏转账：from={}, to={}, amount={}, momentId={}", fromUserId, toUserId, amount, momentId); // 记录开始打赏转账的日志信息

        if (amount.compareTo(BigDecimal.ZERO) <= 0) { // 检查打赏金额是否大于0
            throw new RuntimeException("打赏金额必须大于0"); // 如果金额不大于0，抛出异常
        }

        // 锁一：按 userId 升序锁定，避免两个方向同时转账时产生死锁
        long firstId = Math.min(fromUserId, toUserId); // 获取较小的用户ID
        long secondId = Math.max(fromUserId, toUserId); // 获取较大的用户ID

        // 锁二：获取第一个用户钱包信息并加锁
        Wallet firstWallet = walletMapper.selectByUserIdForUpdate(firstId);
        if (firstWallet == null) { // 检查钱包是否存在
            throw new RuntimeException("钱包不存在，userId: " + firstId); // 如果钱包不存在，抛出异常
        }
        // 锁二：获取第二个用户钱包信息并加锁
        Wallet secondWallet = walletMapper.selectByUserIdForUpdate(secondId);
        if (secondWallet == null) { // 检查钱包是否存在
            throw new RuntimeException("钱包不存在，userId: " + secondId); // 如果钱包不存在，抛出异常
        }

        // 确定哪个是从/哪个是到
        Wallet fromWallet = firstId == fromUserId ? firstWallet : secondWallet; // 根据用户ID确定转出方钱包
        Wallet toWallet = firstId == fromUserId ? secondWallet : firstWallet; // 根据用户ID确定接收方钱包

        if (fromWallet.getBalance().compareTo(amount) < 0) { // 检查转出方余额是否足够
            throw new RuntimeException("余额不足，当前余额: " + fromWallet.getBalance() + "，需要: " + amount); // 如果余额不足，抛出异常
        }

        // 锁三：扣款（乐观锁）
        int deductResult = walletMapper.deductBalance(fromUserId, amount); // 从转出方扣款
        if (deductResult == 0) { // 检查扣款是否成功
            throw new RuntimeException("扣款失败，余额不足或并发冲突"); // 如果扣款失败，抛出异常
        }

        // 到账
        int addResult = walletMapper.addBalance(toUserId, amount); // 向接收方增加余额
        if (addResult == 0) { // 检查到账是否成功
            throw new RuntimeException("到账失败"); // 如果到账失败，抛出异常
        }

        // 记录流水
        BigDecimal fromAfter = fromWallet.getBalance().subtract(amount); // 计算转出方余额变化后的值
        BigDecimal toAfter = toWallet.getBalance().add(amount); // 计算接收方余额变化后的值

        // 插入转出方交易记录
        walletMapper.insertRecord(fromUserId, amount.negate(), 3, momentId,
                fromWallet.getBalance(), fromAfter, "打赏帖子" + momentId);
        // 插入接收方交易记录
        walletMapper.insertRecord(toUserId, amount, 4, momentId,
                toWallet.getBalance(), toAfter, "收到打赏，帖子" + momentId);

        log.info("打赏转账成功：from={} -> to={}, amount={}", fromUserId, toUserId, amount); // 记录打赏转账成功的日志信息
    }

}