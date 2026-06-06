package com.zzzlew.domain.entity;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/15 - 11 - 15 - 0:09
 * @Description: com.zzzlew.zzzimserver.pojo.entity
 * @version: 1.0
 */

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

@Data
public class Apply implements Serializable {

    private Long applyId;

    private Long fromUserId;

    private Long toUserId;

    private String applyMsg;

    private LocalDateTime createTime;

    /**
     * 0：未处理，1：已处理
     */
    private Integer isDealt;

    /**
     * 0：拒绝，1：同意
     */
    private Integer dealResult;

    private LocalDateTime dealTime;

}
