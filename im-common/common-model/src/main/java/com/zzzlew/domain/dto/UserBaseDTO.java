package com.zzzlew.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * 用户基础信息DTO
 */
@Data
public class UserBaseDTO implements Serializable {
    private Long id;

    private String username;

    private String account;

    private String avatar;

    private Integer gender;

    private String phone;
}
