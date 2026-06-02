package com.zzzlew.enums;

import lombok.Getter;

/**
 * @Auther: zzzlew
 * @Date: 2026/1/10 - 01 - 10 - 0:36
 * @Description: com.zzzlew.zzzimserver.enums
 * @version: 1.0
 */
@Getter
public enum ConversationTypeEnum {
    USER(0, "单聊会话"), GROUP(1, "群聊会话"), AI(2, "ai会话");

    /**
     * 会话类型的数值编码
     */
    private final int type;
    /**
     * 会话类型的文字描述
     */
    private final String name;

    ConversationTypeEnum(int type, String name) {
        this.type = type;
        this.name = name;
    }
}
