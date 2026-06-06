package com.zzzlew.server;


import com.zzzlew.domain.dto.DealApplyDTO;
import com.zzzlew.domain.dto.DealGroupDTO;
import com.zzzlew.domain.dto.GroupApplyDTO;
import com.zzzlew.domain.dto.SendApplyDTO;
import com.zzzlew.domain.vo.ApplyVO;
import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.domain.vo.DealApplyVO;
import com.zzzlew.domain.vo.GroupApplyVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/14 - 11 - 14 - 22:34
 * @Description: com.zzzlew.zzzimserver.server
 * @version: 1.0
 */
public interface ApplyService {

    /**
     * 发送好友申请
     *
     * @param sendApplyDTO 好友申请信息
     */
    Long sendApply(SendApplyDTO sendApplyDTO);

    /**
     * 获取好友申请列表
     *
     * @return 好友申请列表
     */
    List<ApplyVO> getApplyList();

    /**
     * 同意好友申请
     *
     * @param dealApplyDTO 好友申请信息
     */
    DealApplyVO dealApply(DealApplyDTO dealApplyDTO);

    /**
     * 获取群聊申请列表
     *
     * @return 群聊申请列表
     */
    List<GroupApplyVO> getGroupApplyList();

    /**
     * 同意入群申请
     *
     * @param dealGroupDTO 群聊申请处理信息
     */
    ConversationVO dealGroupApply(DealGroupDTO dealGroupDTO, MultipartFile groupAvatarBlob);

    /**
     * 发送群聊申请
     *
     * @param userId           用户id
     * @param friendIdList     好友id列表
     * @param groupApplyDTO 群聊申请信息
     */
    void sendGroupApply(Long userId, List<Long> friendIdList, GroupApplyDTO groupApplyDTO);

}
