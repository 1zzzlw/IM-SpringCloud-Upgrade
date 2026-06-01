package com.zzzlew.server.impl;

import cn.hutool.core.bean.BeanUtil;
import com.zzzlew.domain.entity.Conversation;
import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.domain.vo.GroupMemberVO;
import com.zzzlew.enums.ConversationTypeEnum;
import com.zzzlew.mapper.AIMessageMapper;
import com.zzzlew.mapper.ConversationMapper;
import com.zzzlew.mapper.GroupConversationMapper;
import com.zzzlew.mapper.UserMapper;
import com.zzzlew.pojo.entity.GroupConversation;
import com.zzzlew.pojo.entity.UserAuth;
import com.zzzlew.server.ConversationService;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
public class ConversationImpl implements ConversationService {

    @Resource
    private ConversationMapper conversationMapper;
    @Resource
    private UserMapper userMapper;
    @Resource
    private GroupConversationMapper groupConversationMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private AIMessageMapper aiMessageMapper;

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
            List<UserAuth> userAuthList = userMapper.selectUserAuthListByUserIdList(targetUserIdList);
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
                    groupConversationMapper.selectGroupConversationListByConversationIdList(groupIdList);
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
        // TODO 清除会话中未读消息计数
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
        groupConversationMapper.deleteGroupMember(conversationId, userId);
    }

    @Override
    public void createConversation(String conversationId, Long toUserId, String fromUserId, Integer type) {
        conversationMapper.insertConversation(conversationId, toUserId, fromUserId, type);
    }

}
