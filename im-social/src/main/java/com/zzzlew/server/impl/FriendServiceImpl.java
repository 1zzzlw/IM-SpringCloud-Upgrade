package com.zzzlew.server.impl;


import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.vo.FriendRelationVO;
import com.zzzlew.domain.vo.UserSearchVO;
import com.zzzlew.mapper.FriendMapper;
import com.zzzlew.mapper.UserInfoMapper;
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
    private UserInfoMapper userInfoMapper;
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
    public UserSearchVO search(String phone) {
        // 获得当前登录用户的信息，以便查询和好友的状态关系
        Long userId = UserHolder.getUser().getId();

        // 查询用户信息表，根据手机号或账号查询用户信息
        UserSearchVO userSearchVO = userInfoMapper.getByPhoneOrAccount(userId, phone);
        // 打印查询到的用户信息
        log.info("userSearchVO: {}", userSearchVO);
        // 检查用户是否存在
        if (userSearchVO == null) {
            log.info("用户不存在");
            return null;
        }
        if (userSearchVO.getIsFriend() == 0) {
            // 不是好友关系
            log.info("不是好友关系");
        } else {
            // 是好友关系
            log.info("是好友关系");
        }
        return userSearchVO;
    }

    @Override
    public void deleteFriend(String friendId) {
        Long userId = UserHolder.getUser().getId();
        Long friendId1 = Long.parseLong(friendId);
        friendMapper.deleteFriend(userId, friendId1);
        friendMapper.deleteFriend(friendId1, userId);
    }

}
