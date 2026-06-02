package com.zzzlew.controller;


import com.zzzlew.domain.dto.DealApplyDTO;
import com.zzzlew.domain.dto.DealGroupDTO;
import com.zzzlew.domain.dto.GroupApplyDTO;
import com.zzzlew.domain.dto.SendApplyDTO;
import com.zzzlew.domain.vo.ApplyVO;
import com.zzzlew.domain.vo.ConversationVO;
import com.zzzlew.domain.vo.DealApplyVO;
import com.zzzlew.domain.vo.GroupApplyVO;
import com.zzzlew.result.Result;
import com.zzzlew.server.ApplyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/14 - 11 - 14 - 22:33
 * @Description: com.zzzlew.zzzimserver.controller
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/apply")
@Tag(name = "好友申请接口")
public class ApplyController {

    @Resource
    private ApplyService applyService;

    /**
     * 发送好友申请
     *
     * @param sendApplyDTO 好友申请信息
     */
    @Operation(summary = "发送好友申请")
    @PostMapping("/send")
    public Result<Long> sendApply(@RequestBody SendApplyDTO sendApplyDTO) {
        log.info("添加好友，申请信息：{}", sendApplyDTO);
        Long applyId = applyService.sendApply(sendApplyDTO);
        return Result.success(applyId);
    }

    @Operation(summary = "发送群聊申请")
    @PostMapping("/groupApply")
    public Result<Object> sendGroupApply(@RequestParam("userId") Long userId,
                                        @RequestParam("friendIdList") List<Long> friendIdList,
                                        @RequestBody GroupApplyDTO groupApplyDTO) {
        applyService.sendGroupApply(userId, friendIdList, groupApplyDTO);
        return Result.success();
    }

    /**
     * TODO 获取好友申请发送历史
     *
     * @return 好友申请发送历史
     */
    @Operation(summary = "获取好友申请发送历史")
    @GetMapping("/sendHistory")
    public Result<Object> getSendHistory() {
        return Result.success();
    }

    /**
     * 获取好友申请列表
     *
     * @return 好友申请列表
     */
    @Operation(summary = "获取好友申请列表")
    @GetMapping("/list")
    public Result<List<ApplyVO>> getApplyList() {
        List<ApplyVO> applyList = applyService.getApplyList();
        return Result.success(applyList);
    }

    /**
     * 获取群聊申请列表
     *
     * @return 群聊申请列表
     */
    @Operation(summary = "获取群聊申请列表")
    @GetMapping("/groupApplyList")
    public Result<List<GroupApplyVO>> getGroupApplyList() {
        List<GroupApplyVO> groupApplyVOList = applyService.getGroupApplyList();
        return Result.success(groupApplyVOList);
    }

    /**
     * 处理好友申请
     *
     * @param dealApplyDTO 好友申请处理信息
     */
    @Operation(summary = "处理好友申请")
    @PostMapping("/deal")
    public Result<DealApplyVO> dealApply(@RequestBody DealApplyDTO dealApplyDTO) {
        log.info("处理好友申请，申请信息为：{}", dealApplyDTO);
        DealApplyVO dealApplyVO = applyService.dealApply(dealApplyDTO);
        return Result.success(dealApplyVO);
    }

    /**
     * 同意入群申请
     *
     * @param dealGroupDTO 入群申请处理信息
     */
    @Operation(summary = "同意入群申请")
    @PostMapping("/groupApply/deal")
    public Result<ConversationVO> dealGroupApply(@RequestBody DealGroupDTO dealGroupDTO,
                                                 @RequestParam(value = "groupAvatarBlob") MultipartFile groupAvatarBlob) {
        log.info("处理群聊申请：{}, {}", dealGroupDTO, groupAvatarBlob);
        ConversationVO conversationVO = applyService.dealGroupApply(dealGroupDTO, groupAvatarBlob);
        return Result.success(conversationVO);
    }

}
