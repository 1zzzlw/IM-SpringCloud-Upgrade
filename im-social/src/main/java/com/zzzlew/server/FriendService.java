package com.zzzlew.server;


import com.zzzlew.vo.FriendRelationVO;
import com.zzzlew.domain.vo.UserSearchVO;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/12 - 11 - 12 - 23:06
 * @Description: com.zzzlew.zzzimserver.server
 * @version: 1.0
 */
public interface FriendService {

    /**
     * 全量更新并初始化好友列表
     * 
     * @param isInit 是否初始化
     * @return 好友列表
     */
    List<FriendRelationVO> initFriendList(Boolean isInit);

    /**
     * 搜索用户
     * 
     * @param phone 手机号
     * @return 用户搜索vo
     */
    UserSearchVO search(String phone);

    /**
     * 删除好友
     *
     * @param friendId 好友id
     */
    void deleteFriend(String friendId);
}
