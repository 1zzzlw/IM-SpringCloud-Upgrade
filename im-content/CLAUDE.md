# CLAUDE.md — im-content

## 模块职责

收藏/笔记微服务，端口 **8083**，Nacos 注册名 `im-content`。纯粹的 CRUD 微服务，无 MQ、无缓存策略。

## 端点一览（`/favorites`）

| 方法 | 路径 | 说明 |
|------|------|------|
| `POST` | `/favorites/uploadImage` | 上传图片到 MinIO（`zzz-im-favorite` 桶） |
| `POST` | `/favorites/saveNote` | 新建笔记（type=0）`useGeneratedKeys` 返回 ID |
| `POST` | `/favorites/updateNote` | 更新笔记（WHERE id AND user_id 防越权） |
| `GET` | `/favorites/getNote` | 获取当前用户所有笔记（type=0） |
| `POST` | `/favorites/save` | 收藏任意内容（type=1） |
| `GET` | `/favorites/list` | 获取全部收藏（ORDER BY created_at DESC） |
| `DELETE` | `/favorites/delete/{id}` | 删除收藏（WHERE id AND user_id 防越权） |

## 类型区分

- `type=0` → 笔记（用户自建）
- `type=1` → 收藏（收藏他人内容）

## 关键类（仅 4 个源文件）

- [FavoritesController.java](src/main/java/com/zzzlew/controller/FavoritesController.java)
- [FavoritesServiceImpl.java](src/main/java/com/zzzlew/server/impl/FavoritesServiceImpl.java)
- [FavoritesMapper.java](src/main/java/com/zzzlew/mapper/FavoritesMapper.java)
- [FavoritesMapper.xml](src/main/resources/mapper/FavoritesMapper.xml)

## 数据模型

**`favorites` 表**：id, user_id, title, content（JSON 格式）, source_username, type（0/1）, created_at, updated_at

## 安全注意

- 更新和删除操作 SQL 都加了 `AND user_id = #{userId}` 条件，防止跨用户越权操作
- 当前用户从 `UserHolder.getUser()` 获取

## 依赖

- `common-web`（UserHolder、MinIOFileStorgeUtil、Result）
- `common-storage`（MinIO 收藏图片上传）
- `im-api`（Feign 客户端接口库——供其他服务调用此模块）
- `mysql-connector-java`、`mybatis-spring-boot-starter`、`nacos-discovery`
