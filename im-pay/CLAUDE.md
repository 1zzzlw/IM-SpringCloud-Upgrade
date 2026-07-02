# CLAUDE.md — im-pay

## 模块职责

钱包/支付微服务，端口 **8087**，Nacos 注册名 `im-pay`。负责钱包管理、余额操作、打赏转账、红包到账。**无 Feign 客户端依赖**（不被其他服务调用的场景），所有外部调用通过 HTTP 或 MQ。

## 技术标签

- 三层资金安全锁策略（MySQL 悲观锁 FOR UPDATE → 乐观锁 WHERE balance ≥ amount → Redisson 分布式锁幂等）
- MQ 异步转账架构（打赏转账 + 红包到账，支付与业务解耦）
- 流水不可变审计记录（before_balance → after_balance 完整资金追溯链）
- 事务内双层校验（先 SELECT FOR UPDATE 锁行，再 UPDATE WHERE balance ≥ amount 二次拦截溢出）
- Redisson SetNX 分布式锁幂等（MQ 消息级别防重复消费，30s TTL）
- 钱包余额冻结字段预留（freeze_balance 支持 TCC 两阶段提交扩展）

> 我设计并实现了一套基于"悲观锁 + 乐观锁 + 分布式锁"三层防护的支付钱包微服务，通过 SELECT FOR UPDATE 串行化并发钱包操作，通过 WHERE balance ≥ amount 乐观条件二次拦截余额溢出，通过 Redisson SetNX 实现 MQ 消息级别幂等防重复消费，配合 before/after balance 完整流水记录实现资金操作的可追溯与可审计，本质上是一个**具备强一致性和可审计性的金融级资金管理微服务**。

## 端点一览（`/wallet`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `GET` | `/wallet/info` | 查询钱包（余额 + 冻结余额） |
| `GET` | `/wallet/balance` | 只查余额（兼容旧接口） |
| `POST` | `/wallet/recharge` | 充值 `@Transactional`（写流水 type=1） |
| `POST` | `/wallet/withdraw` | 提现 `@Transactional`（写流水 type=2） |
| `GET` | `/wallet/records` | 分页查询流水（支持 type 过滤，0=全部） |
| `POST` | `/wallet/deduct` | **内部接口** — 红包扣款（供 im-chat Feign 调用） |

## 流水类型

| type | 名称 | 触发场景 |
|------|------|----------|
| 1 | 充值 | `POST /wallet/recharge` |
| 2 | 提现 | `POST /wallet/withdraw` |
| 3 | 打赏支出 | MQ `transferForReward` → 发送方 |
| 4 | 打赏收入 | MQ `transferForReward` → 接收方 |
| 5 | 红包支出 | `POST /wallet/deduct` → 发红包者 |
| 6 | 红包收入 | MQ `RedPacketGrabListener` → 抢红包者 |

## MQ 消费者

### RewardMessageListener — 打赏转账

- 监听 `im-reward-create-queue`（routing key `im.reward.create`）
- 幂等校验：`DistributedLockUtil.executeIfAbsent("reward:" + idempotentKey, 300s)`
- `transferForReward(fromUserId, toUserId, amount, momentId)` **`@Transactional`**
  - `SELECT ... FOR UPDATE` 锁双方钱包
  - 乐观扣款 `UPDATE SET balance = balance - amount WHERE balance >= amount`
  - 加款 `UPDATE SET balance = balance + amount`
  - 写流水 type=3 + type=4
- 结果发到 `im-reward-result-queue`（routing key `im.reward.result`）

### RedPacketGrabListener — 红包到账

- 监听 `im-redpacket-grab-queue`（routing key `im.redpacket.grab`）
- 从 Map 提取 userId、redPacketId、amount
- `FOR UPDATE` 锁钱包 → 加款 → 写流水 type=6

## 三层锁策略

1. **MySQL 悲观锁**：`SELECT ... FOR UPDATE` 在事务首行，串行化同一钱包的并发操作
2. **MySQL 乐观锁**：`WHERE balance >= #{amount}` 在 UPDATE 时二次校验
3. **Redisson 分布式锁**：`executeIfAbsent()` 用于 MQ 幂等（非钱包互斥）

## 关键类

- [WalletServiceImpl.java](src/main/java/com/zzzlew/service/impl/WalletServiceImpl.java) — 核心：`transferForReward()` 打赏转账
- [WalletController.java](src/main/java/com/zzzlew/controller/WalletController.java) — REST 端点 + 内部 `/wallet/deduct`
- [RewardMessageListener.java](src/main/java/com/zzzlew/listener/RewardMessageListener.java) — 打赏 MQ 消费者
- [RedPacketGrabListener.java](src/main/java/com/zzzlew/listener/RedPacketGrabListener.java) — 红包 MQ 消费者
- [RewardQueueConfig.java](src/main/java/com/zzzlew/config/RewardQueueConfig.java) — 声明 3 个持久队列
- [WalletMapper.xml](src/main/resources/mapper/WalletMapper.xml) — `FOR UPDATE` / 乐观扣款 SQL

## 数据库

- `wallet`：id, user_id (UNIQUE), balance, freeze_balance, create_time, update_time
- `wallet_record`：id, user_id, amount, type, business_id, before_balance, after_balance, remark, create_time

## 依赖

- `common-web`（DistributedLockUtil、UserHolder、Result、PageResult）
- `redisson-spring-boot-starter`（分布式锁，父 POM 管理版本 3.27.2）
- `spring-boot-starter-amqp`（打赏/红包 MQ 消费者）
- `mysql-connector-java`、`mybatis-spring-boot-starter`、`nacos-discovery`
- **不依赖** `im-api`——没有本模块的 Feign 客户端需要分发
