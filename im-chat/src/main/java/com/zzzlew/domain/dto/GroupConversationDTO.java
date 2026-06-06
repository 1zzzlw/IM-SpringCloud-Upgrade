package com.zzzlew.domain.dto;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/22 - 11 - 22 - 20:17
 * @Description: com.zzzlew.zzzimserver.pojo.dto.conversation
 * @version: 1.0
 */
@Data
public class GroupConversationDTO {

    /**
     * 群聊会话ID
     */
    private String id;

    /**
     * 群聊名称
     */
    private String groupName;

    /**
     * 群聊头像
     */
    private String groupAvatar;

    /**
     * 群聊群主ID
     */
    private Long ownerId;

}
