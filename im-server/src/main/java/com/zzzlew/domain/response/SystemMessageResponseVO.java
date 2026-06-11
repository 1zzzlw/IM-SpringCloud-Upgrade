package com.zzzlew.domain.response;

import com.zzzlew.domain.Message;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/10 - 03 - 10 - 21:10
 * @Description: com.zzzlew.domain.response
 * @version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class SystemMessageResponseVO extends Message implements Serializable {

    /**
     * 系统消息ID
     */
    private String id;

    /**
     * 操作者ID (谁发起的：如群主、撤回者)
     */
    private Long senderId;

    /**
     * 会话ID
     */
    private String conversationId;

    /**
     * 接收者ID (群ID或对方用户ID)
     */
    private String receiverId;    // 接收者ID (群ID或对方用户ID)

    /**
     * 系统消息固定为99
     */
    private Integer msgType = 99;

    /**
     * 系统消息子类型
     */
    private Integer subType;

    /**
     * 结构：{"opId":1, "opName":"张三", "trId":2, "trName":"李四", "ext":{}}
     */
    private String content;

    /**
     * 发送时间
     */
    private LocalDateTime sendTime;


    @Override
    public int getMessageType() {
        return SystemMessageResponseVO;
    }
}
