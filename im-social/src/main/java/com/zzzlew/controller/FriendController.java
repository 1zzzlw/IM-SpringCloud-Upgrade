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
}
