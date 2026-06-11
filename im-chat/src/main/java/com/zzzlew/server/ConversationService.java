package com.zzzlew.server;


import com.zzzlew.domain.dto.GroupApplyDTO;
import com.zzzlew.domain.dto.GroupMemberDTO;
import com.zzzlew.domain.entity.GroupConversation;
import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.domain.vo.GroupMemberVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/21 - 11 - 21 - 21:40
 * @Description: com.zzzlew.zzzimserver.server
 * @version: 1.0
 */
public interface ConversationService {
    /**
     * 全量更新并初始化会话列表
     *
     * @param isInit 是否初始化
     * @return 会话列表
     */
    List<ConversationVO> initConversationList(Boolean isInit);

    /**
     * 获取群成员列表
     *
     * @param conversationId 会话id
     * @return 群成员列表
     */
    List<GroupMemberVO> getGroupMemberList(String conversationId);

    /**
     * 清空会话未读消息数量
     *
     * @param conversationId 会话id
     */
    void clearConversationUnreadCounts(String conversationId);

    /**
     * 更新会话置顶状态
     */
    void updateConversationTopStatus(String conversationId, Integer isTop);

    /**
     * 获取会话免打扰状态
     */
    void updateConversationMuteStatus(String conversationId, Integer isMute);

    /**
     * 删除会话
     */
    void deleteConversation(String conversationId);

    /**
     * 删除群成员
     */
    void deleteGroupMember(String conversationId);

    /**
     * 创建会话
     */
    void createConversation(String conversationId, Long toUserId, String fromUserId, Integer type);

    /**
     * 邀请好友入群
     */
    void inviteFriends(GroupMemberDTO groupMemberDTO);

    /**
     * 创建群聊
     */
    ConversationVO createGroupConversation(List<Long> friendIdList, GroupApplyDTO groupCreateDTO, MultipartFile groupAvatar);

    /**
     * 更新群聊信息
     */
    void updateGroupInfo(String conversationId, String groupAvatar);

    /**
     * 查询会话
     */
    ConversationVO queryConversation(String conversationId);

    /**
     * 踢出群成员（群主/管理员操作）
     */
    void kickMember(String conversationId, Long targetUserId);

    /**
     * 解散群聊（仅群主可操作）
     */
    void dissolveGroup(String conversationId);

    /**
     * 设置/撤销管理员（仅群主可操作）
     */
    void setAdmin(String conversationId, Long targetUserId, Integer role);

    /**
     * 禁言/解除禁言成员
     */
    void muteMember(String conversationId, Long targetUserId, Integer isMute);

    /**
     * 转让群主（仅群主可操作）
     */
    void transferOwner(String conversationId, Long newOwnerId);

    /**
     * 更新群聊信息（名称、描述等）
     */
    void updateGroupInfoFull(String conversationId, String groupName, String groupAvatar, String groupDesc);

    /**
     * 查询群聊详情
     */
    GroupConversation getGroupDetail(String conversationId);

    /**
     * 批量邀请成员入群
     */
    void batchInviteMembers(String conversationId, List<Long> userIds);

    /**
     * 内部服务调用 — 更新群头像（跳过用户鉴权）
     */
    void updateGroupAvatarInternal(String conversationId, String groupAvatar);
}
