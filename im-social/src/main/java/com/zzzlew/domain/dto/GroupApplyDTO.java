package com.zzzlew.domain.dto;

import lombok.Data;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/22 - 11 - 22 - 17:36
 * @Description: com.zzzlew.zzzimserver.pojo.dto.apply
 * @version: 1.0
 */
@Data
public class GroupApplyDTO {

    /**
     * 群会话id
     * 
     */
    private String conversationId;

    /**
     * 申请id集合
     */
    private List<Long> invitedIds;

    /**
     * 群主头像
     */
    private String userAvatar;

    /**
     * 群聊名称
     */
    private String groupName;

}
