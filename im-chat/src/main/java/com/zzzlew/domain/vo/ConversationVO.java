package com.zzzlew.domain.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/21 - 11 - 21 - 21:30
 * @Description: com.zzzlew.zzzimserver.pojo.vo.conversation
 * @version: 1.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationVO {
    /**
     * 会话ID
     */
    private String id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 用户名/群聊名称
     */
    private String name;

    /**
     * 备注
     */
    private String remark;

    /**
     * 用户头像/群聊头像
     */
    private String avatar;

    /**
     * 目标id
     */
    private String targetId;

    /**
     * 会话类型
     */
    private Integer type;

    /**
     * 是否置顶
     */
    private Integer isTop;

    /**
     * 是否免打扰
     * */
    private  Integer isMute;

    /**
     * 未读消息数量
     */
    private Integer unreadCount;

    /**
     * 最后一条消息
     */
    private String latestMsg;

    /**
     * 最后一条消息时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime latestMsgTime;

    /**
     * 会话状态
     */
    private Integer status;

    /**
     * 创建时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 更新时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
}
