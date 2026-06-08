package com.zzzlew.controller;

import com.zzzlew.domain.dto.MomentCommentsDTO;
import com.zzzlew.domain.dto.MomentCommentsPageQueryDTO;
import com.zzzlew.domain.dto.MomentsDTO;
import com.zzzlew.domain.vo.MomentsCommentsVO;
import com.zzzlew.domain.vo.MomentsVO;
import com.zzzlew.result.PageResult;
import com.zzzlew.result.Result;
import com.zzzlew.server.MomentsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

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
     * 上传文本中的图片
     *
     * @param images
     * @return
     */
    @Operation(summary = "上传帖子照片")
    @PostMapping("/uploadImage")
    public Result<List<String>> uploadImage(@RequestParam("images") List<MultipartFile> images) {
        log.info("上传图片：{}", images);
        List<String> urlList = momentsService.uploadImage(images);
        return Result.success(urlList);
    }

    /**
     * 发布朋友圈
     *
     * @param momentsDTO 朋友圈内容
     * @return 发布结果
     */
    @Operation(summary = "发布朋友圈")
    @PostMapping("/publish")
    public Result<Object> publish(@RequestBody MomentsDTO momentsDTO) {
        log.info("用户发布朋友圈：{}", momentsDTO);
        momentsService.publish(momentsDTO);
        return Result.success();
    }

    /**
     * 查看朋友圈
     *
     * @param sortWay 排序方式
     * @param lastId  最后一条数据的id
     * @return 查看结果
     */
    @Operation(summary = "查看朋友圈")
    @GetMapping("/list")
    public Result<List<MomentsVO>> list(@RequestParam(required = false, defaultValue = "0") Integer sortWay,
                                        @RequestParam Long lastId) {
        log.info("查看朋友圈，排序方式：{}，最后一个帖子的id：{}", sortWay, lastId);
        List<MomentsVO> momentsVOList = momentsService.list(sortWay, lastId);
        return Result.success(momentsVOList);
    }

    /**
     * 点赞
     *
     * @return 点赞结果
     */
    @Operation(summary = "点赞")
    @PostMapping("/like/{momentId}")
    public Result<Object> like(@PathVariable("momentId") Long momentId) {
        log.info("点赞，帖子id：{}", momentId);
        momentsService.like(momentId);
        return Result.success();
    }

    @Operation(summary = "查看帖子详细")
    @GetMapping("/detail/{momentId}")
    public Result<MomentsVO> detail(@PathVariable("momentId") Long momentId) {
        log.info("查看帖子详细，帖子id：{}", momentId);
        return Result.success(momentsService.getById(momentId));
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

    /**
     * 分页查看评论
     *
     * @return 评论结果
     */
    @Operation(summary = "分页查看评论")
    @GetMapping("/comments/query")
    public Result<PageResult<MomentsCommentsVO>> comments(MomentCommentsPageQueryDTO momentCommentsPageQueryDTO) {
        log.info("分页查看评论：{}", momentCommentsPageQueryDTO);
        PageResult<MomentsCommentsVO> pageResult = momentsService.comments(momentCommentsPageQueryDTO);
        return Result.success(pageResult);
    }

    /**
     * 查看评论的下级回复列表
     *
     * @return 评论结果
     */
    @Operation(summary = "查看评论的下级回复列表")
    @GetMapping("/comment/reply/{commentId}")
    public String commentReply(@PathVariable("commentId") Long commentId) {
        return "查看评论的下级回复列表成功";
    }

    /**
     * 发布评论
     *
     * @return 发布评论结果
     */
    @Operation(summary = "发布评论")
    @PostMapping("/comment/publish")
    public Result<MomentsCommentsVO> publishComment(@RequestBody MomentCommentsDTO momentCommentsDTO) {
        log.info("发布评论：{}", momentCommentsDTO);
        MomentsCommentsVO vo = momentsService.publishComment(momentCommentsDTO);
        return Result.success(vo);
    }

    /**
     * 发布评论下的回复
     *
     * @return 发布评论下的回复结果
     */
    @Operation(summary = "发布评论下的回复")
    @PostMapping("/comment/reply/publish/{commentId}")
    public String publishCommentReply(@PathVariable("commentId") Long commentId) {
        return "发布评论下的回复成功";
    }

}
