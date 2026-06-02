package com.zzzlew.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2026/1/10 - 01 - 10 - 0:58
 * @Description: com.zzzlew.zzzimserver.pojo.entity
 * @version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GroupConversation extends Conversation {

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
     * 群聊成员数量
     */
    private Integer memberCount;

    /**
     * 群聊最大成员数量
     */
    private Integer maxMember;

    /**
     * 是否全员静音 0：否，1：是
     */
    private Integer isMuteAll;

    /**
     * 群聊描述
     */
    private String groupDesc;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
