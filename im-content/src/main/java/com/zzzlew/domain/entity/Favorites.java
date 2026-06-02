package com.zzzlew.domain.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2026/2/21 - 02 - 21 - 21:01
 * @Description: com.zzzlew.pojo.entity
 * @version: 1.0
 */
@Data
public class Favorites implements Serializable {
    /**
     * 收藏ID
     */
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 收藏标题
     */
    private String title;

    /**
     * 收藏内容(JSON格式)
     */
    private String content;

    /**
     * 来源用户名称
     */
    private String sourceUsername;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    /**
     * 修改时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}
