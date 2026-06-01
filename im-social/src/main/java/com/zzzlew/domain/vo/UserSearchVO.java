package com.zzzlew.domain.vo;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/13 - 11 - 13 - 23:11
 * @Description: com.zzzlew.zzzimserver.pojo.vo.user
 * @version: 1.0
 */
@Data
public class UserSearchVO {

    /**
     * 用户ID
     */
    private Long id;

    /**
     * 用户名
     */
    private String username;

    /**
     * 账号
     */
    private String account;

    /**
     * 头像
     */
    private String avatar;

    /**
     * 性别 0=男，1=女
     */
    private String gender;
    /**
     * 备注
     */
    private String remark;

    /**
     * 地址
     */
    private String address;

    /**
     * 好友关系 0=陌生关系，1=好友关系
     */
    private Integer isFriend;

}
