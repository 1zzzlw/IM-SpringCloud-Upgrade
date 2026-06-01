package com.zzzlew.mapper;

import com.zzzlew.pojo.dto.message.MessageDTO;
import com.zzzlew.pojo.entity.message;
import com.zzzlew.pojo.vo.message.MessageVO;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/15 - 11 - 15 - 23:53
 * @Description: com.zzzlew.zzzimserver.mapper
 * @version: 1.0
 */
public interface MessageMapper {

    /**
     * 初始化会话内的消息列表
     *
     * @param conversationIdList 会话id列表
     * @param quitTime           退出时间
     * @return 消息VO列表
     */
    List<message> initMessageList(List<String> conversationIdList, String quitTime);


    /**
     * 拉取会话内的消息列表
     *
     * @param conversationId 会话id
     * @param maxMessageId   最大消息id
     * @return 消息VO列表
     */
    List<message> pullMessageList(String conversationId, Long maxMessageId);

    /**
     * 保存消息
     *
     * @param messageDTO 消息DTO
     */
    void saveMessage(MessageDTO messageDTO);

    /**
     * 获取会话内的消息列表
     *
     * @param conversationId 会话id
     * @return 消息VO列表
     */
    List<MessageVO> getMessageList(String conversationId);

    /**
     * 更新文件消息发送状态
     *
     * @param fileId     文件消息id
     * @param sendStatus 发送状态
     */
    void updateSendStatus(String fileId, Integer sendStatus);

    /**
     * 清空历史消息
     *
     * @param senderId           发送者id
     * @param conversationId 会话id
     */
    void clearHistoryMessage(Long senderId, String conversationId);

    /**
     * 删除消息
     *
     * @param conversationId 会话id
     * @param messageId      消息id
     */
    void deleteMessage(String conversationId, Long messageId);

}
