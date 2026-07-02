# CLAUDE.md — common-core

## 职责

**聚合/兼容模块**，无任何代码。依赖 `common-model` + `common-base` + `common-redis`，方便一键引入基础依赖（不含 `common-storage` 和 `common-web`，两者需按需显式引入）。

## 技术标签

- 聚合模块模式（POM-only，零代码，纯依赖传递，降低使用方依赖声明成本）
- 单向依赖传递链设计（model → base → redis 严格单向，不可逆）
- 显式排除仓储/Web层（不含 common-storage 和 common-web，防止隐式引入 Tomcat 或 MinIO 依赖污染）

> 我设计了一个零代码的聚合模块，通过 POM-only 模式将 common-model + common-base + common-redis 打包为一键引入，通过单向依赖传递链保证引入顺序正确，同时显式排除 common-web 和 common-storage——**防止被 Netty 服务或 Gateway 误引入时拉进 Tomcat 依赖，本质上是"'便捷性聚合'与'依赖安全'的平衡设计"**。

## 不包含

- `common-storage`（MinIO 存储）——需要文件存储的服务应单独引入 `common-storage`
- `common-web`（分布式锁、MQ 配置、Web 拦截器）——需要这些功能的模块应单独引入 `common-web`

## 依赖

- `common-model`、`common-base`、`common-redis`
