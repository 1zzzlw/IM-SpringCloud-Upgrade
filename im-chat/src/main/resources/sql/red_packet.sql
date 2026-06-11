-- 红包表（与消息共用ID，消息表中msg_type=10代表红包）
CREATE TABLE IF NOT EXISTS `red_packet` (
    `id` bigint NOT NULL COMMENT '红包ID（对应消息ID）',
    `conversation_id` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '会话ID',
    `sender_id` bigint NOT NULL COMMENT '发送者ID',
    `total_amount` decimal(10,2) NOT NULL COMMENT '红包总金额（元）',
    `total_count` int NOT NULL COMMENT '红包总个数',
    `remain_amount` decimal(10,2) NOT NULL COMMENT '剩余金额（元）',
    `remain_count` int NOT NULL COMMENT '剩余个数',
    `type` tinyint NOT NULL DEFAULT 0 COMMENT '红包类型：0-拼手气红包，1-普通红包（均分）',
    `greeting` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT NULL COMMENT '祝福语',
    `status` tinyint NOT NULL DEFAULT 0 COMMENT '状态：0-进行中，1-已领完，2-已过期退款',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`id`) USING BTREE,
    INDEX `idx_conversation_id`(`conversation_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包表' ROW_FORMAT = DYNAMIC;

-- 红包领取记录表
CREATE TABLE IF NOT EXISTS `red_packet_record` (
    `id` bigint NOT NULL AUTO_INCREMENT COMMENT '记录ID',
    `red_packet_id` bigint NOT NULL COMMENT '红包ID',
    `user_id` bigint NOT NULL COMMENT '领取用户ID',
    `amount` decimal(10,2) NOT NULL COMMENT '领取金额（元）',
    `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '领取时间',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE INDEX `uk_red_packet_user`(`red_packet_id` ASC, `user_id` ASC) USING BTREE,
    INDEX `idx_red_packet_id`(`red_packet_id` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '红包领取记录表' ROW_FORMAT = DYNAMIC;
