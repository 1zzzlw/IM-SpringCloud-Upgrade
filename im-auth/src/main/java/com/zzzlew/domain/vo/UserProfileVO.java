package com.zzzlew.domain.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;
import java.time.LocalDate;

/** 当前用户的安全资料，避免把密码等认证字段返回给前端。 */
@Data
public class UserProfileVO {
    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;
    private String username;
    private String account;
    private String avatar;
    private Integer gender;
    private String phone;
    private String email;
    private LocalDate birthday;
    private String address;
}
