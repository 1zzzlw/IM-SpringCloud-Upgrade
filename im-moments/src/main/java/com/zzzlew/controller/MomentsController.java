package com.zzzlew.controller;

import com.zzzlew.result.Result;
import com.zzzlew.server.MomentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/5 - 06 - 05 - 12:00
 * @Description: com.zzzlew.controller
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/moments")
@Tag(name = "朋友圈模块")
public class MomentsController {

    @Resource
    private MomentsService momentsService;

    /**
     * 发布朋友圈
     *
     * @param content 朋友圈内容
     * @return 发布结果
     */
    @Operation(summary = "发布朋友圈")
    @PostMapping("/publish")
    public Result<Object> publish(String content) {
        log.info("用户发布朋友圈：{}", content);
        momentsService.publish(content);
        return Result.success();
    }

    /**
     * 查看朋友圈
     *
     * @return 查看结果
     */
    @Operation(summary = "查看朋友圈")
    @GetMapping("/list")
    public String list() {
        return "查看成功";
    }

    /**
     * 点赞
     *
     * @return 点赞结果
     */
    @Operation(summary = "点赞")
    @PostMapping("/like")
    public String like() {
        return "点赞成功";
    }

    /**
     * 取消点赞
     *
     * @return 取消点赞结果
     */
    @Operation(summary = "取消点赞")
    @PostMapping("/cancelLike")
    public String cancelLike() {
        return "取消点赞成功";
    }

    /**
     * 评论
     *
     * @return 评论结果
     */
    @Operation(summary = "评论")
    @PostMapping("/comment")
    public String comment() {
        return "评论成功";
    }
}
