package com.zzzlew.controller;

import com.zzzlew.result.Result;
import com.zzzlew.server.FriendService;
import com.zzzlew.domain.vo.FriendRelationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/12 - 11 - 12 - 22:51
 * @Description: com.zzzlew.zzzimserver.controller
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/friend")
@Tag(name = "好友接口")
public class FriendController {

    @Resource
    private FriendService friendService;

    /**
     * 全量更新并初始化好友列表
     *
     * @param isInit 是否初始化
     * @return 好友列表
     */
    @Operation(summary = "全量更新并初始化好友列表")
    @GetMapping("/init/list")
    public Result<List<FriendRelationVO>> initFriendList(@RequestParam Boolean isInit) {
        log.info("初始化好友列表: {}", isInit);
        List<FriendRelationVO> friendRelationVOList = friendService.initFriendList(isInit);
        return Result.success(friendRelationVOList);
    }

    /**
     * 删除好友
     *
     * @param friendId 好友id
     */
    @Operation(summary = "删除好友")
    @DeleteMapping("/delete")
    public Result<Object> deleteFriend(String friendId) {
        log.info("删除好友id {}", friendId);
        friendService.deleteFriend(friendId);
        return Result.success();
    }

    /**
     * 更新好友备注
     *
     * @param friendId 好友id
     * @param remark   备注
     */
    @Operation(summary = "更新好友备注")
    @PutMapping("/remark")
    public Result<Object> updateRemark(String friendId, String remark) {
        log.info("更新好友备注, 好友id: {}, 备注: {}", friendId, remark);
        friendService.updateRemark(friendId, remark);
        return Result.success();
    }

    /**
     * 更新好友关系状态
     *
     * @param friendId       好友id
     * @param relationStatus 关系状态
     */
    @Operation(summary = "更新好友关系状态")
    @PutMapping("/status")
    public Result<Object> updateRelationStatus(String friendId, Integer relationStatus) {
        log.info("更新好友关系状态, 好友id: {}, 状态: {}", friendId, relationStatus);
        friendService.updateRelationStatus(friendId, relationStatus);
        return Result.success();
    }
}
