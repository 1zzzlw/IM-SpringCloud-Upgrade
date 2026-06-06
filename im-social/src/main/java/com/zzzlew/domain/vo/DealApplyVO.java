package com.zzzlew.domain.vo;

import lombok.Data;

import java.io.Serializable;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/31 - 03 - 31 - 16:08
 * @Description: com.zzzlew.pojo.vo.apply
 * @version: 1.0
 */
@Data
public class DealApplyVO implements Serializable {

    /**
     * 群聊会话id
     */
    private String conversationId;

    /**
     * 是否在线
     */
    private Integer isOnline;

}
