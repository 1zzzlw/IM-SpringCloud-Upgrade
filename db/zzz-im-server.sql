/*
 Navicat Premium Data Transfer

 Source Server         : localhost
 Source Server Type    : MySQL
 Source Server Version : 80045
 Source Host           : localhost:3306
 Source Schema         : zzz-im-server

 Target Server Type    : MySQL
 Target Server Version : 80045
 File Encoding         : 65001

 Date: 22/06/2026 03:11:44
*/

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for ai_message
-- ----------------------------
DROP TABLE IF EXISTS `ai_message`;
CREATE TABLE `ai_message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT 'AI消息ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '角色: user/assistant',
  `msg_type` tinyint NOT NULL DEFAULT 1 COMMENT '消息类型: 1文本 2图片识别',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '消息内容',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '图片URL（MinIO）',
  `personality_id` bigint NULL DEFAULT NULL COMMENT '使用的AI性格ID',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_created`(`user_id` ASC, `send_time` ASC) USING BTREE,
  INDEX `idx_personality`(`personality_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2040342432062783488 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI对话消息表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for ai_personality
-- ----------------------------
DROP TABLE IF EXISTS `ai_personality`;
CREATE TABLE `ai_personality`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '性格ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `name` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '性格名称',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT 'ai头像',
  `system_prompt` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '系统提示词',
  `is_active` tinyint NULL DEFAULT 0 COMMENT '是否当前使用: 0否 1是',
  `is_preset` tinyint NULL DEFAULT 0 COMMENT '是否预设: 0用户创建 1系统预设',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_name`(`user_id` ASC, `id` ASC) USING BTREE,
  INDEX `uk_active_userId`(`user_id` ASC, `is_active` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 2029758029301420053 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = 'AI个性化配置表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for conversation
-- ----------------------------
DROP TABLE IF EXISTS `conversation`;
CREATE TABLE `conversation`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '会话ID: 私聊id + 群聊id',
  `user_id` bigint NOT NULL COMMENT '当前用户ID',
  `target_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '目标ID: 好友ID 或 群ID',
  `remark` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '会话备注名称',
  `type` tinyint NOT NULL COMMENT '0-私聊, 1-群聊，2-ai会话',
  `is_top` tinyint NULL DEFAULT 0 COMMENT '是否置顶',
  `is_mute` tinyint NULL DEFAULT 0 COMMENT '是否免打扰',
  `unread_count` int NULL DEFAULT 0,
  `latest_msg` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `latest_msg_time` datetime NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 1 COMMENT '1-显示, 0-隐藏（用户删除会话）',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`user_id`, `id`, `status`) USING BTREE,
  INDEX `idx_update_time`(`update_time` ASC) USING BTREE,
  INDEX `idx_user_id_status`(`user_id` ASC, `status` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '会话表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for favorites
-- ----------------------------
DROP TABLE IF EXISTS `favorites`;
CREATE TABLE `favorites`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '收藏ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `title` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '收藏标题',
  `content` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '收藏内容(HTML格式)',
  `source_username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '来源用户名称',
  `type` tinyint NOT NULL DEFAULT 0 COMMENT '0-笔记，1-收藏',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '修改时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_created_at`(`created_at` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 21 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '收藏表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for friend_apply
-- ----------------------------
DROP TABLE IF EXISTS `friend_apply`;
CREATE TABLE `friend_apply`  (
  `apply_id` bigint NOT NULL AUTO_INCREMENT COMMENT '申请唯一ID',
  `from_user_id` bigint NOT NULL COMMENT '申请人ID',
  `to_user_id` bigint NOT NULL COMMENT '被申请人ID',
  `apply_msg` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '申请留言',
  `apply_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '申请发起时间',
  `is_dealt` tinyint NOT NULL DEFAULT 0 COMMENT '是否处理：0=未处理，1=已处理',
  `deal_result` tinyint NULL DEFAULT NULL COMMENT '处理结果：0=拒绝，1=同意',
  `deal_time` datetime NULL DEFAULT NULL COMMENT '处理时间',
  PRIMARY KEY (`apply_id`) USING BTREE,
  UNIQUE INDEX `idx_unique_apply`(`from_user_id` ASC, `to_user_id` ASC) USING BTREE,
  INDEX `idx_to_dealt`(`to_user_id` ASC, `is_dealt` ASC) USING BTREE,
  INDEX `idx_from_user`(`from_user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 107 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友申请流程表（双表设计配套）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for friend_relation
-- ----------------------------
DROP TABLE IF EXISTS `friend_relation`;
CREATE TABLE `friend_relation`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '表id',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `friend_id` bigint NOT NULL COMMENT '好友ID',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '成为好友的时间',
  `remark` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '对好友的备注名',
  `relation_status` tinyint NOT NULL DEFAULT 1 COMMENT '关系状态：0=未同意，1=正常好友，2=拉黑',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '状态更新时间（比如拉黑/恢复好友时自动更新）',
  PRIMARY KEY (`id`, `user_id`, `friend_id`) USING BTREE,
  INDEX `idx_user_status`(`user_id` ASC, `relation_status` ASC) USING BTREE,
  INDEX `idx_friend_id`(`friend_id` ASC) USING BTREE,
  INDEX `idx_user_id_friend_id`(`user_id` ASC, `friend_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 138 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '好友关系核心表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for group_apply
-- ----------------------------
DROP TABLE IF EXISTS `group_apply`;
CREATE TABLE `group_apply`  (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '群会话id',
  `user_id` bigint NOT NULL COMMENT '群主ID',
  `user_avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL DEFAULT '' COMMENT '群主头像',
  `member_id` bigint NOT NULL COMMENT '群成员id',
  `group_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '群聊名称',
  `status` tinyint NULL DEFAULT 1 COMMENT '状态：1-待处理 2-入群 3-拒绝',
  `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_member_id`(`member_id` ASC) USING BTREE,
  INDEX `idx_user_id_member_id`(`user_id` ASC, `member_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 99 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '群聊申请表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for group_conversation
-- ----------------------------
DROP TABLE IF EXISTS `group_conversation`;
CREATE TABLE `group_conversation`  (
  `id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '群ID（全局唯一）',
  `group_name` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '群名称',
  `group_avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '群头像路径',
  `owner_id` bigint NOT NULL COMMENT '群主id',
  `member_count` int NULL DEFAULT 1,
  `max_member` int NULL DEFAULT 200 COMMENT '最多成员数量',
  `is_mute_all` tinyint NULL DEFAULT 0 COMMENT '是否全员禁言：0-否，1-是',
  `group_desc` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '群描述',
  `create_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_owner_id`(`owner_id` ASC) USING BTREE,
  INDEX `idx_owner_id_id`(`id` ASC, `owner_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '群聊表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for group_member
-- ----------------------------
DROP TABLE IF EXISTS `group_member`;
CREATE TABLE `group_member`  (
  `group_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '群ID',
  `user_id` bigint NOT NULL COMMENT '群成员id',
  `role` tinyint NULL DEFAULT 0 COMMENT '0-成员, 1-管理员, 2-群主',
  `is_mute` tinyint NULL DEFAULT 0 COMMENT '是否被禁言',
  `join_time` datetime NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`group_id`, `user_id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_id_group_id`(`group_id` ASC, `user_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '群成员表' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for message
-- ----------------------------
DROP TABLE IF EXISTS `message`;
CREATE TABLE `message`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '消息唯一ID（主键）',
  `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID（单聊：小ID_大ID；群聊：群ID）',
  `sender_id` bigint NOT NULL COMMENT '发送者ID（关联用户表）',
  `receiver_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '接收者ID（单聊：用户ID；群聊：群ID）',
  `msg_type` tinyint NOT NULL COMMENT '消息类型（1=文本，2=图片，3=视频，4=音频，5=文件，6=红包，99=系统消息)',
  `sub_type` tinyint NULL DEFAULT NULL COMMENT '（1 = 撤回消息，2= 删除消息，3 = 添加好友成功，4 = 拉黑好友，5 = 取消拉黑，6=加入群聊，7=退出群聊，8=被踢出群聊，9=群聊解散，10=设为管理员，11=撤销管理员，12=禁言成员，13=取消禁言）',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '消息内容（文本存字符串，媒体存URL+元信息）',
  `send_status` tinyint NOT NULL DEFAULT 0 COMMENT '发送状态（0=发送中，1=成功，2=失败）',
  `read_status` tinyint NOT NULL DEFAULT 1 COMMENT '已读状态（0=未读，1=已读）',
  `send_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发送时间',
  `is_revoked` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否撤回（0=否，1=是）',
  `quote_msg_id` bigint NULL DEFAULT NULL COMMENT '引用的消息ID（回复功能）',
  `quote_content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '引用消息内容',
  `quote_msg_type` tinyint NULL DEFAULT NULL COMMENT '引用消息类型',
  `file_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件唯一id',
  `file_name` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '文件名',
  `file_size` bigint NULL DEFAULT 0 COMMENT '文件大小（B）',
  `bucket` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '桶名称',
  `remote_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '远程存储文件路径',
  `local_path` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '本地存储绝对路径（未下载时为 NULL）',
  `remote_url` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '服务端存储地址',
  `preview_base64` mediumtext CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL COMMENT '图片和视频的预览图',
  `download_status` tinyint NULL DEFAULT 0 COMMENT '下载状态：0-未下载，1-已下载，2-下载失败',
  `receive_time` datetime NULL DEFAULT NULL COMMENT '接收时间',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE COMMENT '按会话ID查询聊天记录',
  INDEX `idx_sender_receiver`(`sender_id` ASC, `receiver_id` ASC) USING BTREE COMMENT '按发送者+接收者筛选',
  INDEX `idx_send_time`(`send_time` ASC) USING BTREE COMMENT '按时间排序消息',
  INDEX `idx_conversation_id_send_time`(`conversation_id` ASC, `send_time` ASC) USING BTREE COMMENT '加载最新消息时的联合索引',
  INDEX `idx_file_id`(`file_id` ASC) USING BTREE COMMENT '文件的唯一id索引',
  INDEX `idx_sender_id_conversation_id`(`conversation_id` ASC, `sender_id` ASC) USING BTREE COMMENT '会话id和发送id的联合索引，用于清空聊天记录'
) ENGINE = InnoDB AUTO_INCREMENT = 7473996337006514367 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '消息表（单聊/群聊通用）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for moment_comments
-- ----------------------------
DROP TABLE IF EXISTS `moment_comments`;
CREATE TABLE `moment_comments`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `moment_id` bigint NOT NULL COMMENT '关联的动态ID',
  `user_id` bigint NOT NULL COMMENT '评论者ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评论者昵称',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '评论者头像',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '评论内容',
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  `parent_id` bigint NULL DEFAULT 0 COMMENT '父评论ID（0表示一级评论）',
  `reply_to_user_id` bigint NULL DEFAULT NULL COMMENT '被回复者的用户ID',
  `reply_to_username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '被回复者的昵称',
  `like_count` int NULL DEFAULT 0 COMMENT '点赞数',
  `is_deleted` tinyint NULL DEFAULT 0 COMMENT '0未删除，1软删除',
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_moment_id`(`moment_id` ASC) USING BTREE,
  INDEX `idx_parent_id`(`parent_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 18 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '动态评论表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for moments
-- ----------------------------
DROP TABLE IF EXISTS `moments`;
CREATE TABLE `moments`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` bigint NOT NULL,
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL,
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL,
  `publish_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `like_count` int NULL DEFAULT 0,
  `comment_count` int NULL DEFAULT 0,
  `reward_amount` int UNSIGNED NULL DEFAULT 0 COMMENT '打赏总金额',
  `is_deleted` tinyint(3) UNSIGNED ZEROFILL NULL DEFAULT 000 COMMENT '0 未删除，1 软删除',
  PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 104 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '朋友圈' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for red_packet
-- ----------------------------
DROP TABLE IF EXISTS `red_packet`;
CREATE TABLE `red_packet`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '红包ID',
  `message_id` bigint NOT NULL COMMENT '关联消息ID',
  `sender_id` bigint NOT NULL COMMENT '发送人ID',
  `receiver_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '单聊接收人',
  `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
  `chat_type` tinyint NOT NULL COMMENT '1单聊 2群聊',
  `total_amount` decimal(18, 2) NOT NULL COMMENT '总金额',
  `remain_amount` decimal(18, 2) NOT NULL COMMENT '剩余金额',
  `total_count` int NOT NULL DEFAULT 1 COMMENT '红包总份数',
  `remain_count` int NOT NULL DEFAULT 1 COMMENT '剩余份数',
  `type` tinyint NULL DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '\r\n0进行中\r\n1已领完\r\n2已过期\r\n3已撤回\r\n',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `expire_time` datetime NULL DEFAULT NULL COMMENT '过期时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_message_id`(`message_id` ASC) USING BTREE,
  INDEX `idx_sender_id`(`sender_id` ASC) USING BTREE,
  INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包信息表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for red_packet_record
-- ----------------------------
DROP TABLE IF EXISTS `red_packet_record`;
CREATE TABLE `red_packet_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '领取记录ID',
  `packet_id` bigint NOT NULL COMMENT '红包ID',
  `user_id` bigint NOT NULL COMMENT '领取用户ID',
  `amount` decimal(18, 2) NOT NULL COMMENT '领取金额',
  `receive_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_packet_user`(`packet_id` ASC, `user_id` ASC) USING BTREE,
  INDEX `idx_packet_id`(`packet_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包领取记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for user_auth
-- ----------------------------
DROP TABLE IF EXISTS `user_auth`;
CREATE TABLE `user_auth`  (
  `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '用户id',
  `user_id` bigint NOT NULL COMMENT '用户信息表关联id\r\n',
  `username` varchar(32) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '用户名',
  `account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '账号',
  `gender` tinyint NOT NULL COMMENT '性别（0-未知，1-男，2-女）',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL COMMENT '密码',
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NULL DEFAULT NULL COMMENT '头像图片路径',
  `create_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `account`(`account` ASC) USING BTREE,
  INDEX `user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1080 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '用户常用信息' ROW_FORMAT = COMPACT;

-- ----------------------------
-- Table structure for user_info
-- ----------------------------
DROP TABLE IF EXISTS `user_info`;
CREATE TABLE `user_info`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '用户昵称',
  `account` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '账号',
  `password` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci NOT NULL,
  `avatar` varchar(512) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '头像URL',
  `gender` tinyint NULL DEFAULT 0 COMMENT '性别（0-女，1-男）',
  `phone` varchar(11) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '手机号',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '邮箱',
  `birthday` date NULL DEFAULT NULL COMMENT '生日',
  `address` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT '' COMMENT '地址',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `account`(`account` ASC) USING BTREE COMMENT '账号索引',
  UNIQUE INDEX `phone`(`phone` ASC) USING BTREE COMMENT '手机号索引，用于快速查询',
  UNIQUE INDEX `email`(`email` ASC) USING BTREE COMMENT '邮箱索引，用于快速查询'
) ENGINE = InnoDB AUTO_INCREMENT = 2017274130705027072 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户信息表（存储个人详细信息）' ROW_FORMAT = DYNAMIC;

-- ----------------------------
-- Table structure for wallet
-- ----------------------------
DROP TABLE IF EXISTS `wallet`;
CREATE TABLE `wallet`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '钱包ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `balance` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '余额',
  `freeze_balance` decimal(18, 2) NOT NULL DEFAULT 0.00 COMMENT '冻结金额',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `update_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '用户钱包表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for wallet_record
-- ----------------------------
DROP TABLE IF EXISTS `wallet_record`;
CREATE TABLE `wallet_record`  (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '流水ID',
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `amount` decimal(18, 2) NOT NULL COMMENT '变动金额',
  `type` tinyint NOT NULL COMMENT '\r\n1=充值\r\n2=提现\r\n3=打赏支出\r\n4=打赏收入\r\n5=红包支出\r\n6=红包收入\r\n7=转账支出\r\n8=转账收入\r\n',
  `business_id` bigint NULL DEFAULT NULL COMMENT '业务ID',
  `before_balance` decimal(18, 2) NOT NULL COMMENT '变更前余额',
  `after_balance` decimal(18, 2) NOT NULL COMMENT '变更后余额',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '备注',
  `create_time` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`) USING BTREE,
  INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
  INDEX `idx_business_id`(`business_id` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 1 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '钱包流水表' ROW_FORMAT = Dynamic;

SET FOREIGN_KEY_CHECKS = 1;
