package com.zzzlew.domain.vo;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/5 - 03 - 05 - 22:00
 * @Description: com.zzzlew.pojo.vo.user
 * @version: 1.0
 */
@Data
public class AIPersonalityVO {
    /**
     * 个性化id
     */
    private String id;

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
