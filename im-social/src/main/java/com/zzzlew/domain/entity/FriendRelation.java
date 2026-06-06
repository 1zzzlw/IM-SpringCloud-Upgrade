package com.zzzlew.domain.entity;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/12 - 11 - 12 - 22:57
 * @Description: com.zzzlew.zzzimserver.pojo.entity
 * @version: 1.0
 */
@Data
public class FriendRelation implements Serializable {
    private Long userId;

    private Long friendId;

    // 备注
    private String remark;

    // 关系状态 0：未同意 1：正常好友 2：黑名单
    private Integer relationStatus;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

}
