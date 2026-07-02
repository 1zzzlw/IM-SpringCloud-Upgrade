# CLAUDE.md — common-storage

## 职责

MinIO 对象存储集成。**独立模块**，不依赖其他 im-common 子模块。

## 技术标签

- MinIO 对象存储封装（文件上传/下载/删除/预签名 URL，统一操作入口）
- 多桶存储隔离策略（7 个桶：头像/图片/视频/音频/文件/收藏/朋友圈，按业务域隔离）
- 文件分块服务端合并（MinIO composeObject API，合并后清理源分块）
- 零框架耦合设计（仅 MinIO SDK + spring-web，不引入 Tomcat，可被 Netty 服务安全引用）
- @ConfigurationProperties 桶名类型安全配置（IDE 自动补全，避免配置 key 拼写错误）

> 我设计了一个独立的 MinIO 对象存储工具模块，通过多桶隔离策略（7 个业务桶：头像/图片/视频/音频/文件/收藏/朋友圈）实现不同类型文件的存储隔离与独立权限控制，通过 MinIO composeObject API 实现服务端分块合并（避免客户端合并后重传），通过零框架耦合设计（仅 MinIO SDK + spring-web 无嵌入式 Tomcat）保证模块的极简依赖——**可被任何模块（包括 Netty 服务）安全引入而不会拉进 Web 容器依赖**。

## 关键类

### `utils/MinIOFileStorgeUtil.java`

MinIO 操作工具类（Spring Bean），提供文件上传/下载/删除等操作。具体方法包括：
- 上传头像、图片、视频、音频、文件、收藏图片、朋友圈图片
- 文件分块合并
- 预签名 URL 生成

### `properties/MinIOConfigProperties.java`

`@ConfigurationProperties(prefix = "minio")`：
- `endpoint`、`accessKey`、`secretKey`
- 7 个桶名：avatarBucket、imageBucket、videoBucket、audioBucket、fileBucket、favoriteBucket、momentsBucket
- `imagePath`、`videoPath`、`expireIn`

### `constant/FileTypeConstant.java`

整数常量：`IMAGE=2`、`VIDEO=3`、`AUDIO=4`、`FILE=5`

## 依赖

- `minio` 8.5.1
- `spring-web`（仅 MultipartFile/Servlet API，**不含**嵌入式 Tomcat）
