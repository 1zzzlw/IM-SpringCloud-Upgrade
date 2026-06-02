package com.zzzlew.domain.dto;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/22 - 11 - 22 - 20:24
 * @Description: com.zzzlew.zzzimserver.pojo.dto.user
 * @version: 1.0
 */
@Data
public class GroupMemberDTO {

    /**
     * 关联群会话表id字段
     */
    private String groupId;

    /**
     * 关联用户表id字段
     */
    private Long userId;

    /**
     * 群成员角色字段 0：普通成员 1：管理员 2：群主
     */
    private Integer role;
}
