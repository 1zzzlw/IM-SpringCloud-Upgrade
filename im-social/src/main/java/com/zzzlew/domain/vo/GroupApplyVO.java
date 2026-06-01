package com.zzzlew.domain.vo;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/22 - 11 - 22 - 16:58
 * @Description: com.zzzlew.zzzimserver.pojo.vo.apply
 * @version: 1.0
 */
@Data
public class GroupApplyVO {

    /**
     * 群聊id
     */
    private Long id;

    /**
     * 群会话ID
     */
    private String conversationId;

    /**
     * 群主ID
     */
    private Long userId;

    /**
     * 群主头像
     */
    private String userAvatar;

    /**
     * 群聊名称
     */
    private String groupName;

    /**
     * 申请状态
     */
    private Integer status;
}
