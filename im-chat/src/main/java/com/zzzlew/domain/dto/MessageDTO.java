package com.zzzlew.domain.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/15 - 11 - 15 - 23:50
 * @Description: com.zzzlew.zzzimserver.pojo.dto.message
 * @version: 1.0
 */
@Data
public class MessageDTO implements Serializable {
    /**
     * 消息id
     */
    private Long id;

    /**
     * 接收者id
     */
    private String receiverId;

    /**
     * 会话id
     */
    private String conversationId;

    /**
     * 发送者id
     */
    private Long senderId;

    /**
     * 消息类型 1：文本消息 2：图片消息 3：语音消息 4：视频消息 5：文件消息
     */
    private Integer msgType;

    /**
     * 系统消息子类型
     */
    private Integer subType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送状态 0 -发送中 1 -成功 2 -失败
     */
    private Integer sendStatus;

    /**
     * 发送时间
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime sendTime;

    /**
     * 是否撤回 0：否 1：是
     */
    private Integer isRevoked;

    /**
     * 引用消息id
     */
    private Long quoteMsgId;

    /**
     * 引用消息内容
     */
    private String quoteContent;

    /**
     * 引用消息类型
     */
    private Integer quoteMsgType;

    /**
     * 文件id
     */
    private String fileId;

    /**
     * 文件名
     */
    private String fileName;

    /**
     * 文件大小
     */
    private Long fileSize;

    /**
     * 存储桶名称
     */
    private String bucket;

    /**
     * 远程文件路径
     */
    private String remotePath;

    /**
     * 本地文件路径
     */
    private String localPath;

    /**
     * 远程文件路径
     */
    private String remoteUrl;

    /**
     * 关联红包ID（msg_type=6 时使用）
     */
    private Long redPacketId;

    /**
     * 预览base64
     */
    private String previewBase64;
}
