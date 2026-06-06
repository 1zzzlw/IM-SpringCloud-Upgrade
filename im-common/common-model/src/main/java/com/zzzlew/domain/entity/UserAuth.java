package com.zzzlew.domain.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/6 - 11 - 06 - 23:13
 * @Description: com.zzzlew.zzzimserver.pojo.entity
 * @version: 1.0
 */
@Data
public class UserAuth implements Serializable {

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
     * 账号
     */
    private String account;

    /**
     * 手机号
     */
    private String phone;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别 0-未知 1-男 2-女
     */
    private Integer gender;

    /**
     * 创建时间
     */
    private LocalDateTime create_time;

    /**
     * 修改时间
     */
    private LocalDateTime update_time;
}
