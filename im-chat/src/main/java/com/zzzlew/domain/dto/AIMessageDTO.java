package com.zzzlew.domain.dto;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/4 - 03 - 04 - 23:16
 * @Description: com.zzzlew.pojo.dto.message
 * @version: 1.0
 */
@Data
public class AIMessageDTO {

    /**
     * 消息id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 角色
     */
    private String role;

    /**
     * 消息类型
     */
    private Integer msgType;

    /**
     * 内容
     */
    private String content;

    /**
     * 图片链接
     */
    private String imageUrl;

    /**
     * 个性化id
     */
    private Integer personalityId;
}
