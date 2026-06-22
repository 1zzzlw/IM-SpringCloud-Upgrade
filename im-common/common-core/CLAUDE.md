# CLAUDE.md — common-core

## 职责

**聚合/兼容模块**，无任何代码。依赖 `common-model` + `common-base` + `common-redis`，方便一键引入基础依赖（不含 `common-storage` 和 `common-web`，两者需按需显式引入）。

## 不包含

- `common-storage`（MinIO 存储）——需要文件存储的服务应单独引入 `common-storage`
- `common-web`（分布式锁、MQ 配置、Web 拦截器）——需要这些功能的模块应单独引入 `common-web`

## 依赖

- `common-model`、`common-base`、`common-redis`
