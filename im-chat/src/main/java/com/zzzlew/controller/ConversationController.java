package com.zzzlew.controller;

import com.zzzlew.domain.dto.GroupApplyDTO;
import com.zzzlew.domain.dto.GroupMemberDTO;
import com.zzzlew.domain.entity.GroupConversation;
import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.domain.vo.GroupMemberVO;
import com.zzzlew.result.Result;
import com.zzzlew.server.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/21 - 11 - 21 - 21:00
 * @Description: com.zzzlew.zzzimserver.controller
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/conversation")
@Tag(name = "会话接口")
public class ConversationController {

    @Resource
    private ConversationService conversationService;

    /**
     * 全量更新并初始化会话列表
     *
     * @param isInit 是否初始化
     * @return 会话列表
     */
    @Operation(summary = "全量更新并初始化会话列表")
    @GetMapping("/init/list")
    public Result<List<ConversationVO>> initConversationList(@RequestParam Boolean isInit) {
        log.info("初始化会话列表：{}", isInit);
        List<ConversationVO> conversationVOList = conversationService.initConversationList(isInit);
        log.info("会话列表：{}", conversationVOList);
        return Result.success(conversationVOList);
    }

    /**
     * 获取群聊成员列表
     *
     * @param conversationId 群聊会话ID
     * @return 群聊成员列表
     */
    @Operation(summary = "获取群聊成员列表")
    @GetMapping("/groupMemberList/{conversationId}")
    public Result<List<GroupMemberVO>> getGroupMemberList(@PathVariable String conversationId) {
        List<GroupMemberVO> groupMemberVOList = conversationService.getGroupMemberList(conversationId);
        return Result.success(groupMemberVOList);
    }

    /**
     * 清除会话中未读消息计数
     *
     * @param conversationId 群聊会话ID
     */
    @Operation(summary = "清除会话中未读消息计数")
    @PutMapping("/isReaded/{conversationId}")
    public Result<Object> clearConversationUnreadCounts(@PathVariable String conversationId) {
        conversationService.clearConversationUnreadCounts(conversationId);
        return Result.success();
    }

    /**
     * 更新会话置顶状态
     *
     * @param conversationId 会话ID
     * @param isTop          是否置顶
     */
    @PostMapping("/updateTopStatus")
    public Result<Object> updateConversationTopStatus(@RequestParam("conversationId") String conversationId,
                                                      @RequestParam("isTop") Integer isTop) {
        log.info("更新会话置顶状态：{},{}", conversationId, isTop);
        conversationService.updateConversationTopStatus(conversationId, isTop);
        return Result.success();
    }

    /**
     * 更新会话免打扰状态
     *
     * @param conversationId 会话ID
     * @param isMute         是否免打扰
     */
    @PostMapping("/updateMuteStatus")
    public Result<Object> updateConversationMuteStatus(@RequestParam("conversationId") String conversationId,
                                                       @RequestParam("isMute") Integer isMute) {
        log.info("更新会话免打扰状态：{},{}", conversationId, isMute);
        conversationService.updateConversationMuteStatus(conversationId, isMute);
        return Result.success();
    }

    /**
     * 删除会话
     *
     * @param conversationId 会话ID
     */
    @Operation(summary = "删除会话")
    @DeleteMapping("/delete")
    public Result<Object> deleteConversation(String conversationId) {
        log.info("删除会话id {}", conversationId);
        conversationService.deleteConversation(conversationId);
        return Result.success();
    }

    /**
     * 退出群聊
     *
     * @param conversationId 群聊会话ID
     */
    @Operation(summary = "退出群聊")
    @DeleteMapping("/exitGroup")
    public Result<Object> exitGroup(String conversationId) {
        log.info("退出群聊id {}", conversationId);
        conversationService.deleteGroupMember(conversationId);
        return Result.success();
    }

    /**
     * 创建新的会话
     *
     * @param conversationId 会话ID
     * @param toUserId       对方用户ID
     * @param fromUserId     自己用户ID
     * @param type           会话类型
     */
    @Operation(summary = "创建新的会话")
    @PostMapping("/create")
    public Result<Object> createConversation(@RequestParam("conversationId") String conversationId,
                                             @RequestParam("toUserId") Long toUserId,
                                             @RequestParam("fromUserId") String fromUserId,
                                             @RequestParam("type") Integer type) {
        log.info("创建新的会话id {}", conversationId);
        conversationService.createConversation(conversationId, toUserId, fromUserId, type);
        return Result.success();
    }

    /**
     * 创建群聊
     *
     * @param groupCreateDTO 群聊申请信息
     * @param groupAvatar    群聊头像文件信息
     * @return 创建的会话信息
     */
    @Operation(summary = "创建群聊")
    @PostMapping("/createGroup")
    public Result<ConversationVO> createGroupConversation(GroupApplyDTO groupCreateDTO,
                                                          @RequestParam(value = "groupAvatar") MultipartFile groupAvatar) {
        log.info("创建群聊：{}，群聊名称：{}", groupCreateDTO.getInvitedIds(), groupCreateDTO.getGroupName());
        List<Long> friendIdList = groupCreateDTO.getInvitedIds();
        log.info("好友ID列表：{}", friendIdList);
        ConversationVO conversationVO = conversationService.createGroupConversation(friendIdList, groupCreateDTO, groupAvatar);
        return Result.success(conversationVO);
    }

    /**
     * 邀请好友入群
     *
     * @param groupMemberDTO 群聊申请信息
     */
    @Operation(summary = "邀请好友入群")
    @PostMapping("/inviteFriend")
    public Result<Object> inviteFriends(@RequestBody GroupMemberDTO groupMemberDTO) {
        log.info("邀请好友入群id {}", groupMemberDTO);
        conversationService.inviteFriends(groupMemberDTO);
        return Result.success();
    }


    /**
     * 更新群聊信息（完整：名称、头像、描述）
     *
     * @param conversationId 群聊会话ID
     * @param groupName      群名称
     * @param groupAvatar    群头像
     * @param groupDesc      群描述
     */
    @Operation(summary = "更新群聊完整信息")
    @PostMapping("/updateGroupInfo")
    public Result<Object> updateGroupInfo(
            @RequestParam("conversationId") String conversationId,
            @RequestParam(value = "groupName", required = false) String groupName,
            @RequestParam(value = "groupAvatar", required = false) String groupAvatar,
            @RequestParam(value = "groupDesc", required = false) String groupDesc) {
        log.info("更新群聊信息：{}", conversationId);
        conversationService.updateGroupInfoFull(conversationId, groupName, groupAvatar, groupDesc);
        return Result.success();
    }

    /**
     * 内部服务调用 — 更新群头像（无需用户鉴权，由 Feign 调用方保证合法性）
     */
    @Operation(summary = "内部服务调用-更新群头像", hidden = true)
    @PostMapping("/internal/updateGroupAvatar")
    public Result<Object> updateGroupAvatarInternal(
            @RequestParam("conversationId") String conversationId,
            @RequestParam("groupAvatar") String groupAvatar) {
        log.info("内部调用-更新群头像：{}, {}", conversationId, groupAvatar);
        conversationService.updateGroupAvatarInternal(conversationId, groupAvatar);
        return Result.success();
    }

    /**
     * 查询会话信息
     *
     * @param conversationId 群聊会话ID
     */
    @Operation(summary = "查询会话信息")
    @GetMapping("/query")
    public Result<ConversationVO> queryConversation(@RequestParam("conversationId") String conversationId) {
        log.info("查询会话信息：{}", conversationId);
        ConversationVO conversationVO = conversationService.queryConversation(conversationId);
        return Result.success(conversationVO);
    }

    /**
     * 查询群聊详情
     */
    @Operation(summary = "查询群聊详情")
    @GetMapping("/groupDetail/{conversationId}")
    public Result<GroupConversation> getGroupDetail(@PathVariable String conversationId) {
        log.info("查询群聊详情：{}", conversationId);
        GroupConversation group = conversationService.getGroupDetail(conversationId);
        return Result.success(group);
    }

    /**
     * 踢出群成员（群主/管理员操作）
     */
    @Operation(summary = "踢出群成员")
    @DeleteMapping("/kickMember")
    public Result<Object> kickMember(@RequestParam("conversationId") String conversationId,
                                     @RequestParam("targetUserId") Long targetUserId) {
        log.info("踢出群成员：群={}, 目标用户={}", conversationId, targetUserId);
        conversationService.kickMember(conversationId, targetUserId);
        return Result.success();
    }

    /**
     * 解散群聊（仅群主可操作）
     */
    @Operation(summary = "解散群聊")
    @DeleteMapping("/dissolveGroup")
    public Result<Object> dissolveGroup(@RequestParam("conversationId") String conversationId) {
        log.info("解散群聊：{}", conversationId);
        conversationService.dissolveGroup(conversationId);
        return Result.success();
    }

    /**
     * 设置/撤销管理员（仅群主可操作）
     *
     * @param role 1=设为管理员, 0=撤销管理员
     */
    @Operation(summary = "设置/撤销管理员")
    @PostMapping("/setAdmin")
    public Result<Object> setAdmin(@RequestParam("conversationId") String conversationId,
                                   @RequestParam("targetUserId") Long targetUserId,
                                   @RequestParam("role") Integer role) {
        log.info("设置管理员：群={}, 目标用户={}, 角色={}", conversationId, targetUserId, role);
        conversationService.setAdmin(conversationId, targetUserId, role);
        return Result.success();
    }

    /**
     * 禁言/解除禁言成员
     *
     * @param isMute 1=禁言, 0=解除禁言
     */
    @Operation(summary = "禁言/解除禁言成员")
    @PostMapping("/muteMember")
    public Result<Object> muteMember(@RequestParam("conversationId") String conversationId,
                                     @RequestParam("targetUserId") Long targetUserId,
                                     @RequestParam("isMute") Integer isMute) {
        log.info("禁言成员：群={}, 目标用户={}, isMute={}", conversationId, targetUserId, isMute);
        conversationService.muteMember(conversationId, targetUserId, isMute);
        return Result.success();
    }

    /**
     * 转让群主（仅群主可操作）
     */
    @Operation(summary = "转让群主")
    @PostMapping("/transferOwner")
    public Result<Object> transferOwner(@RequestParam("conversationId") String conversationId,
                                        @RequestParam("newOwnerId") Long newOwnerId) {
        log.info("转让群主：群={}, 新群主={}", conversationId, newOwnerId);
        conversationService.transferOwner(conversationId, newOwnerId);
        return Result.success();
    }

    /**
     * 批量邀请成员入群（群内拉人）
     */
    @Operation(summary = "批量邀请成员入群")
    @PostMapping("/batchInvite")
    public Result<Object> batchInviteMembers(@RequestParam("conversationId") String conversationId,
                                             @RequestBody List<Long> userIds) {
        log.info("批量邀请成员入群：群={}, 用户列表={}", conversationId, userIds);
        conversationService.batchInviteMembers(conversationId, userIds);
        return Result.success();
    }

}