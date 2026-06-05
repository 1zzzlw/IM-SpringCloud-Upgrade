package com.zzzlew.domain.dto;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/5 - 06 - 05 - 23:27
 * @Description: com.zzzlew.domain.dto
 * @version: 1.0
 */
@Data
public class MomentsDTO {
    /**
     * 主键
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名
     */
    private String username;

    /**
     * 用户头像
     */
    private String avatar;

    /**
     * 内容
     */
    private String content;

    /**
     * 发布时间
     */
    private String publishTime;

    /**
     * 点赞数
     */
    private String likeCount;

    /**
     * 评论数
     */
    private String commentCount;
}
