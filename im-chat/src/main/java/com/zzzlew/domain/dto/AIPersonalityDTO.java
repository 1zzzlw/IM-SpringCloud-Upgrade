package com.zzzlew.domain.dto;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/6 - 03 - 06 - 9:21
 * @Description: com.zzzlew.pojo.dto.user
 * @version: 1.0
 */
@Data
public class AIPersonalityDTO {
    /**
     * 个性化id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 个性化角色名称
     */
    private String name;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 系统提示词内容
     */
    private String systemPrompt;

    /**
     * 是否激活
     */
    private Integer isActive;

    /**
     * 是否预设
     * 0 用户创建 1 系统创建
     */
    private Integer isPreset;
}
