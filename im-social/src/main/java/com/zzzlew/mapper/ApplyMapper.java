package com.zzzlew.mapper;


import com.zzzlew.domain.dto.DealApplyDTO;
import com.zzzlew.domain.dto.DealGroupDTO;
import com.zzzlew.domain.dto.GroupApplyDTO;
import com.zzzlew.domain.dto.SendApplyDTO;
import com.zzzlew.domain.vo.ApplyVO;
import com.zzzlew.domain.vo.GroupApplyVO;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/14 - 11 - 14 - 22:24
 * @Description: com.zzzlew.zzzimserver.mapper
 * @version: 1.0
 */
public interface ApplyMapper {

    /**
     * 发送好友申请
     * 
     * @param sendApplyDTO 好友申请信息
     */
    void sendApply(SendApplyDTO sendApplyDTO);

    /**
     * 获取好友申请列表
     * 
     * @param userId 用户id
     * @return 好友申请列表
     */
    List<ApplyVO> getApplyList(Long userId);

    /**
     * 同意好友申请
     * 
     * @param dealApplyDTO 好友申请处理信息
     */
    void dealApply(DealApplyDTO dealApplyDTO);

    /**
     * 发送群聊申请
     *
     * @param userId 用户id
     * @param friendIdList 好友ID列表
     * @param groupApplyDTO 群聊申请信息
     */
    void sendGroupApply(Long userId, List<Long> friendIdList, GroupApplyDTO groupApplyDTO);

    /**
     * 获取群聊申请列表
     *
     * @param userId 用户id
     * @return 群聊申请列表
     */
    List<GroupApplyVO> getGroupApplyList(Long userId);

    /**
     * 处理群聊申请
     *
     * @param dealGroupDTO 群聊申请处理信息
     */
    void dealGroupApply(DealGroupDTO dealGroupDTO);

}
