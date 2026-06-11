package com.zzzlew.server.impl;


import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.vo.FriendRelationVO;
import com.zzzlew.mapper.FriendMapper;
import com.zzzlew.server.FriendService;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static com.zzzlew.constant.RedisConstant.USER_OFFLINE_INFO_KEY;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/12 - 11 - 12 - 23:06
 * @Description: com.zzzlew.zzzimserver.server.impl
 * @version: 1.0
 */
@Slf4j
@Service
public class FriendServiceImpl implements FriendService {

    @Resource
    private FriendMapper friendMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 全量更新并初始化好友列表
     *
     * @param isInit 是否初始化
     * @return 好友列表
     */
    @Override
    public List<FriendRelationVO> initFriendList(Boolean isInit) {
        // 获得当前登录用户的信息
        UserBaseDTO userBaseDTO = UserHolder.getUser();
        // 获取当前用户id
        Long userId = userBaseDTO.getId();
        log.info("当前用户id: {}", userId);
        // 查看redis中是否有登录记录
        Map<Object, Object> entries = stringRedisTemplate.opsForHash().entries(USER_OFFLINE_INFO_KEY);
        String quitTime;
        if (entries.get(userId.toString()) == null || isInit) {
            quitTime = null;
        } else {
            quitTime = entries.get(userId.toString()).toString();
        }
        log.info("该用户的上次离线时间是：{}", quitTime);
        // 根据用户id和用户离线时间查询该用户的好友列表
        List<FriendRelationVO> friendRelationVOList = friendMapper.selectFriendList(userId, quitTime);
        // 打印好友列表
        log.info("好友列表: {}", friendRelationVOList);
        // 返回好友列表
        return friendRelationVOList;
    }

    @Override
    public void deleteFriend(String friendId) {
        Long userId = UserHolder.getUser().getId();
        Long friendId1 = Long.parseLong(friendId);
        friendMapper.deleteFriend(userId, friendId1);
        friendMapper.deleteFriend(friendId1, userId);
    }

    @Override
    public void updateRemark(String friendId, String remark) {
        Long userId = UserHolder.getUser().getId();
        Long friendIdLong = Long.parseLong(friendId);
        friendMapper.updateFriendRemark(userId, friendIdLong, remark);
    }

    @Override
    public void updateRelationStatus(String friendId, Integer relationStatus) {
        Long userId = UserHolder.getUser().getId();
        Long friendIdLong = Long.parseLong(friendId);
        // 更新双向关系状态以保持一致性
        friendMapper.updateFriendRelationStatus(userId, friendIdLong, relationStatus);
        friendMapper.updateFriendRelationStatus(friendIdLong, userId, relationStatus);
    }

}
