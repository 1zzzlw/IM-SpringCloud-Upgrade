# CLAUDE.md — common-model

## 职责

最底层的共享数据模型模块，**无内部依赖**。定义跨服务通用的 API 响应包装、DTO、VO、实体。

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
