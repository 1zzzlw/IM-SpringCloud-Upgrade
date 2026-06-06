package com.zzzlew.domain.dto;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/22 - 11 - 22 - 19:31
 * @Description: com.zzzlew.zzzimserver.pojo.dto.apply
 * @version: 1.0
 */
@Data
public class DealGroupDTO {
    /**
     * 群会话ID
     */
    private String conversationId;

    /**
     * 群主id
     */
    private Long userId;

    /**
     * 群成员id
     */
    private Long memberId;

    /**
     * 入群状态
     */
    private Integer status;

    /**
     * 群头像
     */
    private String userAvatar;

}
