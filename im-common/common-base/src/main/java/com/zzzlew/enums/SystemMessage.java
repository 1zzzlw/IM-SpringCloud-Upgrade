package com.zzzlew.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/10 - 03 - 10 - 21:37
 * @Description: com.zzzlew.enums
 * @version: 1.0
 */
@Getter
@AllArgsConstructor
public enum SystemMessage {
    /**
     * 系统消息 - 消息操作
     */
    RECALL_MESSAGE(1, "撤回消息"), DELETE_MESSAGE(2, "删除消息"),

    /**
     * 系统消息 - 好友关系
     */
    ADD_FRIEND_SUCCESS(3, "添加好友成功"), BLACKLIST_ADD(4, "拉黑好友"), BLACKLIST_REMOVE(5, "取消拉黑"),

    /**
     * 系统消息 - 群聊成员
     */
    JOIN_GROUP(6, "加入群聊"), QUIT_GROUP(7, "退出群聊"), KICK_OUT_GROUP(8, "被踢出群聊"), GROUP_DISMISS(9, "群聊解散"),

    /**
     * 系统消息 - 群聊管理
     */
    SET_ADMIN(10, "设为管理员"), REMOVE_ADMIN(11, "撤销管理员"), MUTE_MEMBER(12, "禁言成员"), UNMUTE_MEMBER(13, "取消禁言");

    /**
     * 消息类型值
     */
    private final Integer value;

    /**
     * 消息描述
     */
    private final String description;

    /**
     * 根据 value 获取枚举
     */
    public static SystemMessage getByValue(Integer value) {
        if (value == null) {
            throw new IllegalArgumentException("消息类型值不能为空");
        }

        return switch (value) {
            case 1 -> RECALL_MESSAGE;
            case 2 -> DELETE_MESSAGE;
            case 3 -> ADD_FRIEND_SUCCESS;
            case 4 -> BLACKLIST_ADD;
            case 5 -> BLACKLIST_REMOVE;
            case 6 -> JOIN_GROUP;
            case 7 -> QUIT_GROUP;
            case 8 -> KICK_OUT_GROUP;
            case 9 -> GROUP_DISMISS;
            case 10 -> SET_ADMIN;
            case 11 -> REMOVE_ADMIN;
            case 12 -> MUTE_MEMBER;
            case 13 -> UNMUTE_MEMBER;
            default -> throw new IllegalArgumentException("没有此类型的值：" + value);
        };
    }

}
