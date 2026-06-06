package com.zzzlew.domain.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/10 - 11 - 10 - 20:42
 * @Description: com.zzzlew.zzzimserver.pojo.entity
 * @version: 1.0
 */
@Data
public class UserInfo implements Serializable {

    private Long id;

    private String username;

    private String account;

    private String password;

    private String avatar;

    private Integer gender;

    private String phone;

    private String email;

    private LocalDate birthday;

    private String address;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
