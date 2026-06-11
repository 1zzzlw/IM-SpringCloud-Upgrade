package com.zzzlew.domain.response;

import com.zzzlew.domain.Message;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/19 - 11 - 19 - 22:54
 * @Description: com.zzzlew.zzzimserver.pojo.vo.message
 * @version: 1.0
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GroupChatResponseVO extends Message implements Serializable {
    /**
     * 消息id
     */
    private String id;

    /**
     * 群聊ID
     */
    private String conversationId;

    /**
     * 发送者ID
     */
    private Long senderId;

    /**
     * 群聊接收id为 'g_群聊ID'
     */
    private String receiverId;

    /**
     * 消息类型
     */
    private Integer msgType;

    /**
     * 消息内容
     */
    private String content;

    /**
     * 发送状态
     */
    private Integer sendStatus;

    /**
     * 读取状态 0：未读 1：已读
     */
    private Integer readStatus;

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
     * 本地url
     */
    private String localPath;

    /**
     * 远程url
     */
    private String remoteUrl;

    /**
     * 预览base64
     */
    private String previewBase64;

    /**
     * 下载状态 0：未下载 1：已下载
     */
    private Integer downloadStatus;

    /**
     * 接收时间
     */
    private LocalDateTime receiveTime;

    @Override
    public int getMessageType() {
        return Message.GroupChatResponseVO;
    }
}
