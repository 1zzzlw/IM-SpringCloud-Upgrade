package com.zzzlew.lock;

import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * 分布式锁工具类（基于 Redisson）
 * 统一供 im-pay、im-chat 等模块使用
 * 仅当 classpath 中存在 RedissonClient 时才激活
 */
@Slf4j
@Component
@ConditionalOnClass(RedissonClient.class)
public class DistributedLockUtil {

    @Resource
    private RedissonClient redissonClient;

    /**
     * 尝试加锁，执行业务后自动释放
     *
     * @param lockKey     锁键
     * @param waitTime    最大等待时间（秒）
     * @param leaseTime   锁持有时间（秒），-1 表示看门狗自动续期
     * @param supplier    需要在锁内执行的业务逻辑
     * @return 业务返回值
     */
    public <T> T tryLock(String lockKey, long waitTime, long leaseTime, Supplier<T> supplier) {
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(waitTime, leaseTime, TimeUnit.SECONDS);
            if (!locked) {
                throw new RuntimeException("获取锁失败，请稍后重试：" + lockKey);
            }
            return supplier.get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("获取锁被中断：" + lockKey, e);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }

    /**
     * 幂等性校验：基于 Redis SET NX EX 语义，key 不存在才执行业务。
     * 执行成功后 key 保留 TTL 秒，期间所有重复请求均被拒绝。
     * 业务失败时删除 key，允许 MQ 重试后再次执行。
     *
     * @param idempotentKey 幂等 key（有业务含义的唯一标识）
     * @param ttl           key 存活时间（秒），期间阻止重复执行
     * @param runnable      需要执行的业务逻辑
     * @return true=业务已执行，false=重复请求已跳过
     */
    public boolean executeIfAbsent(String idempotentKey, long ttl, Runnable runnable) {
        String key = "idempotent:" + idempotentKey;
        // 使用 RBucket.setIfAbsent 实现真正的 SET NX EX，不手动释放
        boolean acquired = redissonClient.getBucket(key).setIfAbsent("1", Duration.ofSeconds(ttl));
        if (!acquired) {
            log.warn("幂等校验：已处理过，跳过 key={}", idempotentKey);
            return false;
        }
        try {
            runnable.run();
            return true;
        } catch (Exception e) {
            // 业务执行失败 → 删除幂等标记，允许 MQ 重试后再次执行
            log.error("幂等业务执行失败，删除标记允许重试，key={}", idempotentKey, e);
            redissonClient.getBucket(key).delete();
            throw e;
        }
    }
}
