package com.zzzlew.mapper;


import com.zzzlew.domain.vo.FriendRelationVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/12 - 11 - 12 - 23:25
 * @Description: com.zzzlew.zzzimserver.mapper
 * @version: 1.0
 */
public interface FriendMapper {
    /**
     * 查询用户好友列表
     *
     * @param userId   用户ID
     * @param quitTime 用户离线时间
     * @return 好友列表
     */
    List<FriendRelationVO> selectFriendList(Long userId, String quitTime);

    /**
     * 添加好友关系
     *
     * @param toUserId   被添加好友用户ID
     * @param fromUserId 添加好友用户ID
     */
    void addFriendToRelation(Long toUserId, Long fromUserId);

    /**
     * 删除好友关系
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     */
    void deleteFriend(Long userId, long friendId);

    /**
     * 更新好友备注
     *
     * @param userId   用户ID
     * @param friendId 好友ID
     * @param remark   备注
     */
    void updateFriendRemark(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("remark") String remark);

    /**
     * 更新好友关系状态
     *
     * @param userId         用户ID
     * @param friendId       好友ID
     * @param relationStatus 关系状态
     */
    void updateFriendRelationStatus(@Param("userId") Long userId, @Param("friendId") Long friendId, @Param("relationStatus") Integer relationStatus);
}
