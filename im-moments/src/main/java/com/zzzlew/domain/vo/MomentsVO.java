package com.zzzlew.domain.vo;

import lombok.Data;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/5 - 06 - 05 - 23:27
 * @Description: com.zzzlew.domain.vo
 * @version: 1.0
 */
@Data
public class MomentsVO {
    /**
     * 主键
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    /**
     * 用户id
     */
    @JsonSerialize(using = ToStringSerializer.class)
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
     * 是否点赞
     */
    private boolean liked;

    /**
     * 点赞数
     */
    private Integer likeCount;

    /**
     * 评论数
     */
    private Integer commentCount;

    /**
     * 累计打赏金额
     */
    private java.math.BigDecimal rewardAmount;
}
