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

    /**
     * 当前成员数量（创建时为1，即群主）
     */
    private Integer memberCount;

    /**
     * 最大成员数量
     */
    private Integer maxMember;

    /**
     * 群聊描述
     */
    private String groupDesc;

}
