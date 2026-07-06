package com.zzzlew.domain.request;

import com.zzzlew.domain.Message;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/19 - 11 - 19 - 16:16
 * @Description: com.zzzlew.zzzimserver.pojo.dto.message
 * @version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GroupChatRequestDTO extends Message implements Serializable {

    /**
     * 消息id
     */
    private Long id;

    /**
     * 群聊会话id
     */
    private String conversationId;

    /**
     * 发送者id
     */
    private Long senderId;

    /**
     * 接收者id，用于存储数据库
     */
    private String receiverId;

    /**
     * 接收者id列表
     */
    private List<Long> receiverIds;

    /**
     * 消息类型
     */
    private Integer msgType;

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

    @Override
    public int getMessageType() {
        return GroupChatRequestDTO;
    }
}
