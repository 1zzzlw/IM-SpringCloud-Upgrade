package com.zzzlew.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/8 - 03 - 08 - 16:52
 * @Description: com.zzzlew.pojo.dto.favorites
 * @version: 1.0
 */
@Data
public class FavoritesVO {

    /**
     * id
     */
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 标题
     */
    private String title;

    /**
     * 内容
     */
    private String content;

    /**
     * 来源用户名
     */
    private String sourceUsername;

    /**
     * 类型 0：笔记 1：收藏
     */
    private Integer type;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
