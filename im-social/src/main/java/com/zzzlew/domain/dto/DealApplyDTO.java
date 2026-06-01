package com.zzzlew.domain.dto;

import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/15 - 11 - 15 - 19:19
 * @Description: com.zzzlew.zzzimserver.pojo.dto.Apply
 * @version: 1.0
 */
@Data
public class DealApplyDTO implements Serializable {

    /**
     * 好友申请id
     */
    private Long applyId;

    /**
     * 申请人id
     */
    private Long fromUserId;

    /**
     * 处理结果
     */
    private Integer dealResult;

    /**
     * 处理时间
     */
    private LocalDateTime dealTime;
}
