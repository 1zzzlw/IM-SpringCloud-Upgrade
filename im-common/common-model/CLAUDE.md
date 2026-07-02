# CLAUDE.md — common-model

## 职责

最底层的共享数据模型模块，**无内部依赖**。定义跨服务通用的 API 响应包装、DTO、VO、实体。

## 技术标签

- 统一 REST 响应模型（Result<T> 泛型 + 工厂方法模式：success / error）
- 分层 VO / DTO / Entity 数据对象规约（视图层/传输层/持久层边界清晰）
- 零框架依赖的轻量级数据模型（仅 fastjson + jackson-annotations，无 Spring）
- 泛型消息包装器（ClusterMessageWrapper<T> —— MQ 跨集群通信的通用载体）
- PageResult<T> 统一分页响应（total + data，全项目分页接口规范）

> 我设计了一个零框架依赖的底层共享数据模型模块，通过 Result<T> 泛型 + 工厂方法模式统一所有微服务的 REST 响应格式（code + msg + data），通过 VO / DTO / Entity 分层规约明确各层数据对象职责，通过 ClusterMessageWrapper<T> 泛型包装实现 MQ 消息的通用承载，本质上是微服务架构中**"数据契约层"的最佳实践——最底层、最稳定、最少依赖**。

## 关键类

| 类 | 用途 |
|----|------|
| `result/Result<T>` | 统一 REST 响应：`code`（1=成功/0=失败）、`msg`、`data`。工厂方法 `success()`、`success(T)`、`error(msg)` |
| `result/PageResult<T>` | 分页响应：`total` + `data` |
| `result/TokenResult` | accessToken + refreshToken 对 |
| `domain/dto/UserBaseDTO` | **核心用户上下文**：id, username, account, avatar, gender, phone。被 UserHolder 和 Feign 拦截器使用 |
| `domain/dto/GroupMemberDTO` | 群成员 DTO |
| `domain/dto/GroupApplyDTO` | 群申请 DTO |
| `domain/dto/DeductDTO` | 扣款 DTO（userId, amount, businessId, remark） |
| `domain/dto/RewardMessageDTO` | 打赏请求 DTO（idempotentKey, momentId, fromUserId, toUserId, amount） |
| `domain/dto/RewardResultDTO` | 打赏结果 DTO（success, failReason） |
| `domain/vo/ConversationVO` | 会话视图对象 |
| `domain/vo/FriendRelationVO` | 好友关系视图对象 |
| `domain/entity/UserAuth` | 用户认证实体（user_id, username, account, phone, avatar, gender） |
| `domain/ClusterMessageWrapper<T>` | MQ 消息通用包装器（message + targetUserId） |

## 依赖

- `fastjson`
- `jackson-annotations`
