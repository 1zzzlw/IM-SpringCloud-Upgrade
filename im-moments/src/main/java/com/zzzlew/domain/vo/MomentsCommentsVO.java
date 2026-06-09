package com.zzzlew.domain.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/7 - 06 - 07 - 20:46
 * @Description: com.zzzlew.domain.vo
 * @version: 1.0
 */
@Data
public class MomentsCommentsVO {
    /**
     * 主键ID
     */
    private Long id;

    /**
     * 关联的动态ID
     */
    private Long momentId;

    /**
     * 评论者用户ID
     */
    private Long userId;

    /**
     * 评论者昵称
     */
    private String username;

    /**
     * 评论者头像
     */
    private String avatar;

    /**
     * 评论内容
     */
    private String content;

    /**
     * 评论时间
     */
    private LocalDateTime publishTime;

    /**
     * 父评论ID（0=一级评论）
     */
    private Long parentId;

    /**
     * 被回复者用户ID
     */
    private Long replyToUserId;

    /**
     * 被回复者昵称
     */
    private String replyToUsername;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 当前用户是否点赞
     */
    private Boolean liked;
}
