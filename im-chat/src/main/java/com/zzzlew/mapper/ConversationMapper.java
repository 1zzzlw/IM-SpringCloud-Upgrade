package com.zzzlew.mapper;


import com.zzzlew.pojo.entity.Conversation;
import com.zzzlew.pojo.vo.user.GroupMemberVO;

import java.time.LocalDateTime;
import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/21 - 11 - 21 - 21:40
 * @Description: com.zzzlew.zzzimserver.mapper
 * @version: 1.0
 */
public interface ConversationMapper {
    /**
     * 根据用户id和用户离线时间列表查询会话列表
     *
     * @param userId 用户id
     * @param quitTime 用户离线时间
     * @return 会话列表
     */
    List<Conversation> selectList(Long userId, String quitTime);

    /**
     * 更新会话状态
     *
     * @param conversationId 会话id
     * @param content 最后一条消息内容
     * @param sendTime 最后一条消息时间
     */
    void updateConversationStatus(String conversationId, String content, LocalDateTime sendTime, String receiverId);

    /**
     * 更新群会话状态
     *
     * @param conversationId 会话id
     * @param content 最后一条消息内容
     * @param sendTime 最后一条消息时间
     */
    void updateGroupConversationStatus(String conversationId, String content,
                                       LocalDateTime sendTime, List<String> receiverIds);

    /**
     * 查询群聊成员列表
     *
     * @param conversationId 群聊会话ID
     * @return 群聊成员列表
     */
    List<GroupMemberVO> selectGroupMemberListByConversationId(String conversationId);

    /**
     * 初始化好友会话
     *
     * @param toUserId 接收方用户ID
     * @param fromUserId 发送方用户ID
     */
    void insertConversation(String conversationId, Long toUserId, String fromUserId, Integer type);

    /**
     * 更新会话置顶状态
     *
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param isTop 是否置顶
     */
    void updateConversationTopStatus(String conversationId, Long userId, Integer isTop);

    /**
     * 更新会话免打扰状态
     *
     * @param conversationId 会话ID
     * @param userId 用户ID
     * @param isMute 是否免打扰
     */
    void updateConversationMuteStatus(String conversationId, Long userId, Integer isMute);

    /**
     * 删除会话
     *
     * @param conversationId 会话ID
     */
    void deleteConversation(String conversationId);
}
