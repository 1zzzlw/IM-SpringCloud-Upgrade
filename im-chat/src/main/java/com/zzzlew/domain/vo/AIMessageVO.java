package com.zzzlew.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/5 - 03 - 05 - 13:45
 * @Description: com.zzzlew.pojo.vo.message
 * @version: 1.0
 */
@Data
public class AIMessageVO {

    /**
     * 消息id
     */
    private String id;

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
     * 图片地址
     */
    private String imageUrl;

    /**
     * 个性化id
     */
    private Integer personalityId;

    /**
     * 发送时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;
}
