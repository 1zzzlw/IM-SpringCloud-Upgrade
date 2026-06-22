# CLAUDE.md — common-storage

## 职责

MinIO 对象存储集成。**独立模块**，不依赖其他 im-common 子模块。

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
