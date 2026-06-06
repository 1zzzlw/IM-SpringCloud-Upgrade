package com.zzzlew.domain.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/14 - 11 - 14 - 22:21
 * @Description: com.zzzlew.zzzimserver.pojo.dto.friend
 * @version: 1.0
 */
@Data
public class SendApplyDTO implements Serializable {

    private Long applyId;

    private Long fromUserId;

    private Long toUserId;

    private String applyMsg;
}
