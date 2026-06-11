package com.zzzlew.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/23 - 11 - 23 - 21:34
 * @Description: com.zzzlew.zzzimserver.pojo.vo.user
 * @version: 1.0
 */
@Data
public class GroupMemberVO implements Serializable {

    /**
     * 群成员id
     */
    private Long userId;

    /**
     * 群成员名称
     */
    private String username;

     /**
      * 群成员角色
      */
    private Integer role;

     /**
      * 群成员头像
      */
    private String avatar;

    /**
     * 是否禁言 0:否 1:是
     */
    private Integer isMute;
}
