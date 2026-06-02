package com.zzzlew.server.impl;

import com.zzzlew.client.ChatClient;
import com.zzzlew.domain.dto.*;
import com.zzzlew.domain.vo.ApplyVO;
import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.domain.vo.DealApplyVO;
import com.zzzlew.domain.vo.GroupApplyVO;
import com.zzzlew.mapper.ApplyMapper;
import com.zzzlew.mapper.FriendMapper;
import com.zzzlew.properties.MinIOConfigProperties;
import com.zzzlew.server.ApplyService;
import com.zzzlew.utils.MinIOFileStorgeUtil;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.zzzlew.constant.RedisConstant.*;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/14 - 11 - 14 - 22:35
 * @Description: com.zzzlew.zzzimserver.server.impl
 * @version: 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplyServiceImpl implements ApplyService {

    private final ChatClient chatClient;

    @Resource
    private ApplyMapper applyMapper;
    @Resource
    private FriendMapper friendMapper;
    @Resource
    private MinIOFileStorgeUtil minIOFileStorgeUtil;
    @Resource
    private MinIOConfigProperties minIOConfigProperties;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Long sendApply(SendApplyDTO sendApplyDTO) {
        // 获得当前登录用户id
        Long userId = UserHolder.getUser().getId();
        log.info("当前登录用户id：{}", userId);
        sendApplyDTO.setFromUserId(userId);
        applyMapper.sendApply(sendApplyDTO);
        // 打印好友申请id
        log.info("好友申请表id: {}", sendApplyDTO.getApplyId());
        return sendApplyDTO.getApplyId();
    }

    @Override
    public List<ApplyVO> getApplyList() {
        // 获得当前登录用户的id
        Long userId = UserHolder.getUser().getId();
        // 根据id查询对应的好友列表
        List<ApplyVO> list = applyMapper.getApplyList(userId);
        log.info("好友申请列表: {}", list);
        return list;
    }

    @Transactional
    @Override
    public DealApplyVO dealApply(DealApplyDTO dealApplyDTO) {
        LocalDateTime dealTime = LocalDateTime.now();
        dealApplyDTO.setDealTime(dealTime);
        applyMapper.dealApply(dealApplyDTO);
        // 如果同意好友申请，需要添加好友到好友列表
        if (dealApplyDTO.getDealResult() == 1) {
            // TODO 增加 “幂等性处理”（避免重复添加好友）

            // 获得当前登录用户id，也就是说处理申请的用户
            Long toUserId = UserHolder.getUser().getId();
            // 申请来自的谁的id，也就是发送申请的用户id
            Long fromUserId = dealApplyDTO.getFromUserId();

            String conversationId = toUserId > fromUserId ? String.format("%d_%d", toUserId, fromUserId) : String.format("%d_%d", fromUserId, toUserId);

            // 插入会话表
            chatClient.createConversation(conversationId, toUserId, fromUserId.toString(), 0);

            chatClient.createConversation(conversationId, fromUserId, toUserId.toString(), 0);

            // 插入好友关系表
            friendMapper.addFriendToRelation(toUserId, fromUserId);
            friendMapper.addFriendToRelation(fromUserId, toUserId);

            // redis中插入好友关系
            // 处理申请的用户id
            String friend1ListKey = USER_FRIEND_LIST_KEY + toUserId;
            // 发送申请的用户id
            String friend2ListKey = USER_FRIEND_LIST_KEY + fromUserId;
            stringRedisTemplate.opsForSet().add(friend1ListKey, fromUserId.toString());
            stringRedisTemplate.opsForSet().add(friend2ListKey, toUserId.toString());
            // 设置好友列表的过期时间
            stringRedisTemplate.expire(friend1ListKey, USER_FRIEND_LIST_KEY_TTL, TimeUnit.MINUTES);
            stringRedisTemplate.expire(friend2ListKey, USER_FRIEND_LIST_KEY_TTL, TimeUnit.MINUTES);

            DealApplyVO dealApplyVO = new DealApplyVO();

            dealApplyVO.setConversationId(conversationId);
            // 查看发送申请用户是否在线
            Boolean isFromUserOnline = stringRedisTemplate.opsForSet().isMember(USER_ONLINE_STATUS_KEY, fromUserId.toString());
            if (Boolean.TRUE.equals(isFromUserOnline)) {
                log.info("对方在线，发送好友申请消息");
                dealApplyVO.setIsOnline(1);
            } else {
                log.info("对方不在线");
                dealApplyVO.setIsOnline(0);
            }
            return dealApplyVO;
        }
        return null;
    }

    @Override
    public List<GroupApplyVO> getGroupApplyList() {
        // 获得当前登录用户id
        Long userId = UserHolder.getUser().getId();
        // 根据id查询对应的群聊申请列表
        List<GroupApplyVO> list = applyMapper.getGroupApplyList(userId);
        log.info("群聊申请列表: {}", list);
        return list;
    }

    @Transactional
    @Override
    public ConversationVO dealGroupApply(DealGroupDTO dealGroupDTO, MultipartFile groupAvatarBlob) {
        // 获得当前登录用户id
        Long userId = UserHolder.getUser().getId();
        String conversationId = dealGroupDTO.getConversationId();
        String groupAvatar = null;
        if (groupAvatarBlob != null && groupAvatarBlob.getSize() > 0) {
            // 生成群聊头像的远端存储路径
            String avatarName = conversationId + ".png";
            String minioGroupAvatarPath = conversationId + "/" + avatarName;
            // 上传用户头像到minio服务端
            minIOFileStorgeUtil.uploadAvatar(minioGroupAvatarPath, groupAvatarBlob);
            // 生成本地存储远程路径
            groupAvatar = minIOConfigProperties.getEndpoint() + "/" + minIOConfigProperties.getAvatarBucket() + "/" + minioGroupAvatarPath;
            dealGroupDTO.setUserAvatar(groupAvatar);
        }
        // 修改群聊申请状态
        applyMapper.dealGroupApply(dealGroupDTO);
        // 更新群会话的头像
        if (dealGroupDTO.getStatus() == 2) {
            // 同意入群申请，需要插入群成员表
            GroupMemberDTO groupMemberDTO = new GroupMemberDTO();
            groupMemberDTO.setGroupId(conversationId);
            groupMemberDTO.setUserId(dealGroupDTO.getMemberId());
            groupMemberDTO.setRole(0);
            chatClient.inviteFriends(groupMemberDTO);
            // 更新群聊会话表的头像
            chatClient.updateGroupInfo(conversationId, groupAvatar);
            // 插入会话列表
            chatClient.createConversation(conversationId, userId, conversationId, 1);
            // 查询群会话列表
            ConversationVO conversationVO = chatClient.queryConversation(conversationId).getData();
            conversationVO.setId(conversationId);
            conversationVO.setTargetId(conversationId);
            conversationVO.setUserId(userId);
            conversationVO.setType(1);
            log.info("群会话信息为：{}", conversationVO);
            return conversationVO;
        } else {
            log.info("用户id：{}拒绝入群申请", userId);
            return null;
        }
    }

    @Override
    public void sendGroupApply(Long userId, List<Long> friendIdList, GroupApplyDTO groupApplyDTO) {
        applyMapper.sendGroupApply(userId, friendIdList, groupApplyDTO);
    }

}
