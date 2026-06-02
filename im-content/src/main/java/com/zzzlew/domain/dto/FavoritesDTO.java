package com.zzzlew.domain.dto;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/8 - 03 - 08 - 16:52
 * @Description: com.zzzlew.pojo.dto.favorites
 * @version: 1.0
 */
@Data
public class FavoritesDTO {

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
}
