package com.zzzlew.controller;

import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.domain.vo.GroupMemberVO;
import com.zzzlew.result.Result;
import com.zzzlew.server.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

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

}