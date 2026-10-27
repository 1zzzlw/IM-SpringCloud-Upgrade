package com.zzzlew.domain.dto;

import lombok.Data;
import java.time.LocalDate;

/** 只接收账号设置中允许修改的字段，用户身份由登录上下文确定。 */
@Data
public class UserProfileUpdateDTO {
    private String account;
    private String email;
    private LocalDate birthday;
}
