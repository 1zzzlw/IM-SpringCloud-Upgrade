package com.zzzlew.domain.vo;

import com.alibaba.fastjson.annotation.JSONField;
import com.alibaba.fastjson.serializer.ToStringSerializer;
import lombok.Builder;
import lombok.Data;

import java.io.Serializable;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/6 - 11 - 06 - 23:29
 * @Description: com.zzzlew.zzzimserver.pojo.vo
 * @version: 1.0
 */
@Builder
@Data
public class UserInfoVO implements Serializable {
    @JSONField(serializeUsing = ToStringSerializer.class)
    private Long id;

    private String username;

    private String account;

    private String avatar;

    private Integer gender;

    private String phone;
}
