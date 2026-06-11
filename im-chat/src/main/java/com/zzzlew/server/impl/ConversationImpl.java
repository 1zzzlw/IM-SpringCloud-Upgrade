package com.zzzlew.server.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.zzzlew.client.AuthClient;
import com.zzzlew.client.SocialClient;
import com.zzzlew.domain.dto.GroupApplyDTO;
import com.zzzlew.domain.dto.GroupConversationDTO;
import com.zzzlew.domain.dto.GroupMemberDTO;
import com.zzzlew.domain.entity.Conversation;
import com.zzzlew.domain.entity.GroupConversation;
import com.zzzlew.domain.entity.UserAuth;
import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.domain.vo.GroupMemberVO;
import com.zzzlew.enums.ConversationTypeEnum;
import com.zzzlew.mapper.AIMessageMapper;
import com.zzzlew.mapper.ConversationMapper;
import com.zzzlew.properties.MinIOConfigProperties;
import com.zzzlew.server.ConversationService;
import com.zzzlew.utils.MinIOFileStorgeUtil;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.zzzlew.constant.RedisConstant.USER_OFFLINE_INFO_KEY;


/**
 * @Auther: zzzlew
 * @Date: 2025/11/21 - 11 - 21 - 21:40
 * @Description: com.zzzlew.zzzimserver.server.impl
 * @version: 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ConversationImpl implements ConversationService {

    private final AuthClient authClient;
    private final SocialClient socialClient;

    @Resource
    private ConversationMapper conversationMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AIMessageMapper aiMessageMapper;
    @Resource
    private MinIOFileStorgeUtil minIOFileStorgeUtil;
    @Resource
    private MinIOConfigProperties minIOConfigProperties;


    /**
     * 全量更新并初始化会话列表
     *
     * @param isInit 是否初始化
     * @return 会话列表
     */
    @Transactional
    @Override
    public List<ConversationVO> initConversationList(Boolean isInit) {
        // 获得当前登录用户id
        Long userId = UserHolder.getUser().getId();
        log.info("初始化会话的id：{}", userId);
        // 查看redis中是否有登录记录
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(USER_OFFLINE_INFO_KEY);
        String quitTime;
        if (entries.get(userId.toString()) == null || isInit) {
            // redis中不存在该值的信息，或者已经标记了是初始化
            quitTime = null;
        } else {
            quitTime = entries.get(userId.toString()).toString();
        }
        // 根据用户id和用户的离线时间查询登录用户的会话列表
        List<Conversation> conversationList = conversationMapper.selectList(userId, quitTime);

        // 如果会话列表为空，直接返回空列表
        if (conversationList.isEmpty()) {
            return List.of();
        }

        List<Long> targetUserIdList = conversationList.stream()
                .filter(conversation -> conversation.getType() == ConversationTypeEnum.USER.getType())
                .map(conversation -> Long.parseLong(conversation.getTargetId())).toList();

        List<String> groupIdList = conversationList.stream()
                .filter(conversation -> conversation.getType() == ConversationTypeEnum.GROUP.getType())
                .map(Conversation::getTargetId).toList();

        // 以用户id为键，用户信息为值
        Map<Long, UserAuth> userMap;

        if (!targetUserIdList.isEmpty()) {
            // 查询用户信息
            List<UserAuth> userAuthList = authClient.getUserListByIds(targetUserIdList).getData();
            // 以用户id为键，用户信息为值
            userMap = userAuthList.stream().collect(Collectors.toMap(UserAuth::getUserId, u -> u));
        } else {
            // 如果用户会话列表为空，直接返回空列表
            userMap = Map.of();
        }

        // 以群聊会话id为键，群聊会话信息为值
        Map<String, GroupConversation> groupMap;

        if (!groupIdList.isEmpty()) {
            // 查询群聊会话信息
            List<GroupConversation> groupConversationList =
                    conversationMapper.selectGroupConversationListByConversationIdList(groupIdList);
            // 以群聊会话id为键，群聊会话信息为值
            groupMap = groupConversationList.stream().collect(Collectors.toMap(GroupConversation::getId, g -> g));
        } else {
            // 如果群聊会话列表为空，直接返回空列表
            groupMap = Map.of();
        }

        log.info("会话列表: {}", conversationList);
        log.info("用户会话列表: {}", userMap);
        log.info("群聊会话列表: {}", groupMap);

        // 转换为VO
        List<ConversationVO> conversationVOList = conversationList.stream().map(conversation -> {
            if (conversation.getType() == ConversationTypeEnum.USER.getType() && !userMap.isEmpty()) {
                // 单聊会话
                Long targetUserId = Long.parseLong(conversation.getTargetId());
                // 获得目标用户信息
                UserAuth targetUser = userMap.get(targetUserId);
                // 转换为VO
                ConversationVO conversationVO = BeanUtil.copyProperties(conversation, ConversationVO.class);
                conversationVO.setAvatar(targetUser.getAvatar());
                conversationVO.setName(targetUser.getUsername());
                conversationVO.setUserId(userId);
                return conversationVO;
            } else if (conversation.getType() == ConversationTypeEnum.GROUP.getType() && !groupMap.isEmpty()) {
                // 群聊会话 获得目标群聊会话信息
                GroupConversation groupConversation = groupMap.get(conversation.getTargetId());
                // 转换为VO
                ConversationVO conversationVO = BeanUtil.copyProperties(conversation, ConversationVO.class);
                conversationVO.setAvatar(groupConversation.getGroupAvatar());
                conversationVO.setName(groupConversation.getGroupName());
                conversationVO.setUserId(userId);
                return conversationVO;
            } else if (conversation.getType() == ConversationTypeEnum.AI.getType()) {
                // ai会话
                ConversationVO conversationVO = BeanUtil.copyProperties(conversation, ConversationVO.class);
                String avatar = aiMessageMapper.getAiAvatarById(userId);
                conversationVO.setAvatar(avatar);
                conversationVO.setName("ai助手");
                conversationVO.setUserId(userId);
                return conversationVO;
            } else {
                throw new IllegalArgumentException("会话类型不存在");
            }
        }).collect(Collectors.toList());

        return conversationVOList;
    }

    @Override
    public List<GroupMemberVO> getGroupMemberList(String conversationId) {
        // 根据用户id和会话id查询登录用户的会话列表
        List<GroupMemberVO> groupMemberVOList =
                conversationMapper.selectGroupMemberListByConversationId(conversationId);
        return groupMemberVOList;
    }

    @Override
    public void clearConversationUnreadCounts(String conversationId) {
        Long userId = UserHolder.getUser().getId();
        conversationMapper.clearUnreadCount(conversationId, userId);
    }

    @Override
    public void updateConversationTopStatus(String conversationId, Integer isTop) {
        Long userId = UserHolder.getUser().getId();
        conversationMapper.updateConversationTopStatus(conversationId, userId, isTop);
    }

    @Override
    public void updateConversationMuteStatus(String conversationId, Integer isMute) {
        Long userId = UserHolder.getUser().getId();
        conversationMapper.updateConversationMuteStatus(conversationId, userId, isMute);
    }

    @Override
    public void deleteConversation(String conversationId) {
        conversationMapper.deleteConversation(conversationId);
    }

    @Override
    public void deleteGroupMember(String conversationId) {
        Long userId = UserHolder.getUser().getId();
        conversationMapper.deleteGroupMember(conversationId, userId);
        conversationMapper.decrGroupMemberCount(conversationId);
    }

    @Override
    public void createConversation(String conversationId, Long toUserId, String fromUserId, Integer type) {
        conversationMapper.insertConversation(conversationId, toUserId, fromUserId, type);
    }

    @Override
    public void inviteFriends(GroupMemberDTO groupMemberDTO) {
        conversationMapper.insertGroupMember(groupMemberDTO);
        // 增加群成员数量
        conversationMapper.incrGroupMemberCount(groupMemberDTO.getGroupId());
    }

    /**
     * 发送群聊申请
     *
     * @param friendIdList   好友ID列表
     * @param groupCreateDTO 群聊申请信息
     */
    @Transactional
    @Override
    public ConversationVO createGroupConversation(List<Long> friendIdList, GroupApplyDTO groupCreateDTO,
                                                  MultipartFile groupAvatarFile) {
        // 生成群聊的唯一id
        long snowflakeId = IdUtil.getSnowflakeNextId();
        String conversationId = "g_" + snowflakeId;
        // 获得当前登录用户id
        Long userId = UserHolder.getUser().getId();

        // 生成群聊头像的远端存储路径
        String avatarName = conversationId + ".png";
        String minioGroupAvatarPath = conversationId + "/" + avatarName;
        // 上传用户头像到minio服务端
        minIOFileStorgeUtil.uploadAvatar(minioGroupAvatarPath, groupAvatarFile);
        // 生成本地存储远程路径
        String groupAvatar = minIOConfigProperties.getEndpoint() + "/" + minIOConfigProperties.getAvatarBucket() + "/" + minioGroupAvatarPath;

        groupCreateDTO.setUserAvatar(groupAvatar);

        groupCreateDTO.setConversationId(conversationId);
        // 插入群聊申请表
        socialClient.sendGroupApply(userId, friendIdList, groupCreateDTO);

        // 插入群聊会话表
        GroupConversationDTO groupConversationDTO = new GroupConversationDTO();
        groupConversationDTO.setId(conversationId);
        groupConversationDTO.setGroupName(groupCreateDTO.getGroupName());
        groupConversationDTO.setGroupAvatar(groupAvatar);
        groupConversationDTO.setOwnerId(userId);
        groupConversationDTO.setMemberCount(1); // 群主自己
        groupConversationDTO.setMaxMember(200);
        groupConversationDTO.setGroupDesc("");

        conversationMapper.insertGroupConversation(groupConversationDTO);

        // 插入群成员表
        GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
        groupMemberDTO.setGroupId(conversationId);
        groupMemberDTO.setUserId(userId);
        groupMemberDTO.setRole(2);
        conversationMapper.insertGroupMember(groupMemberDTO);

        // 插入会话表
        conversationMapper.insertConversation(conversationId, userId, conversationId, 1);

        ConversationVO conversationVO = ConversationVO.builder().id(conversationId).avatar(groupAvatar).name(groupCreateDTO.getGroupName()).userId(userId).targetId(conversationId).type(1).build();

        return conversationVO;
    }

    @Override
    public void updateGroupInfo(String conversationId, String groupAvatar) {
        conversationMapper.updateGroupConversation(conversationId, groupAvatar);
    }

    @Override
    public ConversationVO queryConversation(String conversationId) {
        return conversationMapper.selectGroupConversation(conversationId);
    }

    @Override
    public void kickMember(String conversationId, Long targetUserId) {
        Long currentUserId = UserHolder.getUser().getId();
        // 检查操作者是否是群主或管理员
        Integer currentRole = conversationMapper.selectMemberRole(conversationId, currentUserId);
        if (currentRole == null || currentRole == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有群主或管理员才能踢人");
        }
        // 不能踢群主
        Long ownerId = conversationMapper.selectGroupOwner(conversationId);
        if (targetUserId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能踢出群主");
        }
        // 管理员不能踢管理员
        Integer targetRole = conversationMapper.selectMemberRole(conversationId, targetUserId);
        if (currentRole == 1 && targetRole != null && targetRole == 1) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "管理员不能踢出其他管理员");
        }
        conversationMapper.kickGroupMember(conversationId, targetUserId);
        conversationMapper.decrGroupMemberCount(conversationId);
    }

    @Transactional
    @Override
    public void dissolveGroup(String conversationId) {
        Long currentUserId = UserHolder.getUser().getId();
        Long ownerId = conversationMapper.selectGroupOwner(conversationId);
        if (!currentUserId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有群主才能解散群聊");
        }
        // 删除所有群成员
        conversationMapper.deleteAllGroupMembers(conversationId);
        // 删除群会话记录（conversation表中该群相关记录）
        conversationMapper.deleteGroupConversationRecords(conversationId);
        // 删除群聊会话表记录
        conversationMapper.deleteGroupConversationById(conversationId);
    }

    @Override
    public void setAdmin(String conversationId, Long targetUserId, Integer role) {
        Long currentUserId = UserHolder.getUser().getId();
        Long ownerId = conversationMapper.selectGroupOwner(conversationId);
        if (!currentUserId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有群主才能设置管理员");
        }
        conversationMapper.updateMemberRole(conversationId, targetUserId, role);
    }

    @Override
    public void muteMember(String conversationId, Long targetUserId, Integer isMute) {
        Long currentUserId = UserHolder.getUser().getId();
        Integer currentRole = conversationMapper.selectMemberRole(conversationId, currentUserId);
        if (currentRole == null || currentRole == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有群主或管理员才能禁言");
        }
        // 不能禁言群主
        Long ownerId = conversationMapper.selectGroupOwner(conversationId);
        if (targetUserId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "不能禁言群主");
        }
        conversationMapper.updateMemberMute(conversationId, targetUserId, isMute);
    }

    @Transactional
    @Override
    public void transferOwner(String conversationId, Long newOwnerId) {
        Long currentUserId = UserHolder.getUser().getId();
        Long ownerId = conversationMapper.selectGroupOwner(conversationId);
        if (!currentUserId.equals(ownerId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有群主才能转让群主");
        }
        // 新群主必须是群成员
        Integer targetRole = conversationMapper.selectMemberRole(conversationId, newOwnerId);
        if (targetRole == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "目标用户不是群成员");
        }
        // 转让群主
        conversationMapper.transferGroupOwner(conversationId, newOwnerId);
        // 原群主变为普通成员（role=0），新群主变为群主（role=2）
        conversationMapper.updateMemberRole(conversationId, currentUserId, 0);
        conversationMapper.updateMemberRole(conversationId, newOwnerId, 2);
    }

    @Override
    public void updateGroupInfoFull(String conversationId, String groupName, String groupAvatar, String groupDesc) {
        Long currentUserId = UserHolder.getUser().getId();
        Integer currentRole = conversationMapper.selectMemberRole(conversationId, currentUserId);
        // 只有群主和管理员可以修改群信息
        if (currentRole == null || currentRole == 0) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "只有群主或管理员才能修改群信息");
        }
        conversationMapper.updateGroupInfoFull(conversationId, groupName, groupAvatar, groupDesc);
    }

    @Override
    public GroupConversation getGroupDetail(String conversationId) {
        return conversationMapper.selectGroupDetail(conversationId);
    }

    @Override
    @Transactional
    public void batchInviteMembers(String conversationId, List<Long> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "邀请成员列表不能为空");
        }

        // 权限校验：调用者必须是群成员
        Long currentUserId = UserHolder.getUser().getId();
        Integer callerRole = conversationMapper.selectMemberRole(conversationId, currentUserId);
        if (callerRole == null) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "您不是该群成员");
        }

        // 批量插入群成员（role=0 普通成员，INSERT IGNORE 自动跳过已存在的成员）
        conversationMapper.batchInsertGroupMembers(conversationId, userIds, 0);

        // 批量创建会话记录
        conversationMapper.batchInsertConversations(conversationId, userIds, conversationId, 1);

        // 更新群成员数量（按实际插入数量）
        conversationMapper.incrGroupMemberCountBy(conversationId, userIds.size());
    }

    @Override
    public void updateGroupAvatarInternal(String conversationId, String groupAvatar) {
        // 内部服务调用，不校验用户权限，直接更新群头像
        conversationMapper.updateGroupInfoFull(conversationId, null, groupAvatar, null);
    }


}
