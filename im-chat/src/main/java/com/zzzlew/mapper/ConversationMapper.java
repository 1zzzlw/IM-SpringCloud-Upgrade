package com.zzzlew.mapper;


import com.zzzlew.domain.dto.GroupConversationDTO;
import com.zzzlew.domain.dto.GroupMemberDTO;
import com.zzzlew.domain.entity.Conversation;
import com.zzzlew.domain.entity.GroupConversation;
import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.domain.vo.GroupMemberVO;
import org.apache.ibatis.annotations.Param;

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
     * @param userId   用户id
     * @param quitTime 用户离线时间
     * @return 会话列表
     */
    List<Conversation> selectList(Long userId, String quitTime);

    /**
     * 更新会话状态
     *
     * @param conversationId 会话id
     * @param content        最后一条消息内容
     * @param sendTime       最后一条消息时间
     */
    void updateConversationStatus(String conversationId, String content, LocalDateTime sendTime, String receiverId);

    /**
     * 更新群会话状态
     *
     * @param conversationId 会话id
     * @param content        最后一条消息内容
     * @param sendTime       最后一条消息时间
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
     * @param toUserId   接收方用户ID
     * @param fromUserId 发送方用户ID
     */
    void insertConversation(String conversationId, Long toUserId, String fromUserId, Integer type);

    /**
     * 更新会话置顶状态
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param isTop          是否置顶
     */
    void updateConversationTopStatus(String conversationId, Long userId, Integer isTop);

    /**
     * 更新会话免打扰状态
     *
     * @param conversationId 会话ID
     * @param userId         用户ID
     * @param isMute         是否免打扰
     */
    void updateConversationMuteStatus(String conversationId, Long userId, Integer isMute);

    /**
     * 删除会话
     *
     * @param conversationId 会话ID
     */
    void deleteConversation(String conversationId);

    /**
     * 插入群聊会话
     *
     * @param groupConversationDTO 群聊会话信息
     */
    void insertGroupConversation(GroupConversationDTO groupConversationDTO);

    /**
     * 插入群成员
     *
     * @param groupMemberDTO 群成员信息
     */
    void insertGroupMember(GroupMemberDTO groupMemberDTO);

    /**
     * 更新群聊会话的群成员数量
     *
     * @param conversationId 群聊会话ID
     * @param groupAvatar    群聊会话头像
     */
    void updateGroupConversation(String conversationId, String groupAvatar);

    /**
     * 根据群聊会话ID列表查询群聊会话列表
     *
     * @param groupIdList 群聊会话ID列表
     * @return 群聊会话列表
     */
    List<GroupConversation> selectGroupConversationListByConversationIdList(List<String> groupIdList);

    /**
     * 根据群id查群群会话信息
     *
     * @param conversationId 群聊会话ID
     * @return 群聊会话信息
     */
    ConversationVO selectGroupConversation(String conversationId);

    /**
     * 根据群id查询群成员id列表
     *
     * @param conversationId 群聊会话ID
     * @return 群成员id集合
     */
    List<String> selectGroupNumber(String conversationId);

    /**
     * 删除群成员
     *
     * @param conversationId 群聊会话ID
     */
    void deleteGroupMember(String conversationId, Long userId);

    /**
     * 群成员数量+1
     *
    */
    void incrGroupMemberCount(String groupId);

    /**
     * 群成员数量-1
     */
    void decrGroupMemberCount(String groupId);

    /**
     * 查询群主ID
     */
    Long selectGroupOwner(String conversationId);

    /**
     * 查询某成员在群中的角色
     */
    Integer selectMemberRole(String conversationId, Long userId);

    /**
     * 更新成员角色
     */
    void updateMemberRole(@Param("conversationId") String conversationId,
                          @Param("userId") Long userId,
                          @Param("role") Integer role);

    /**
     * 更新成员禁言状态
     */
    void updateMemberMute(@Param("conversationId") String conversationId,
                          @Param("userId") Long userId,
                          @Param("isMute") Integer isMute);

    /**
     * 踢出群成员
     */
    void kickGroupMember(@Param("conversationId") String conversationId,
                         @Param("userId") Long userId);

    /**
     * 删除群所有成员
     */
    void deleteAllGroupMembers(String conversationId);

    /**
     * 删除群会话记录
     */
    void deleteGroupConversationRecords(String conversationId);

    /**
     * 删除群聊会话表记录
     */
    void deleteGroupConversationById(String conversationId);

    /**
     * 清除会话未读消息数
     */
    void clearUnreadCount(@Param("conversationId") String conversationId,
                          @Param("userId") Long userId);

    /**
     * 更新群聊完整信息（名称、头像、描述）
     */
    void updateGroupInfoFull(@Param("conversationId") String conversationId,
                             @Param("groupName") String groupName,
                             @Param("groupAvatar") String groupAvatar,
                             @Param("groupDesc") String groupDesc);

    /**
     * 转让群主
     */
    void transferGroupOwner(@Param("conversationId") String conversationId,
                            @Param("newOwnerId") Long newOwnerId);

    /**
     * 查询群聊详情
     */
    GroupConversation selectGroupDetail(String conversationId);

    /**
     * 批量插入群成员
     */
    void batchInsertGroupMembers(@Param("groupId") String groupId,
                                  @Param("userIds") List<Long> userIds,
                                  @Param("role") Integer role);

    /**
     * 批量增加群成员数量
     */
    void incrGroupMemberCountBy(@Param("groupId") String groupId,
                                 @Param("count") Integer count);

    /**
     * 批量插入会话记录
     */
    void batchInsertConversations(@Param("conversationId") String conversationId,
                                   @Param("userIds") List<Long> userIds,
                                   @Param("targetId") String targetId,
                                   @Param("type") Integer type);
}
