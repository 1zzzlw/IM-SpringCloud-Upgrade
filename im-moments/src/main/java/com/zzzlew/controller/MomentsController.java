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
     * @param lastId 最后一条数据的id
     * @return 查看结果
     */
    @Operation(summary = "查看最新发布的朋友圈")
    @GetMapping("/list/new")
    public Result<List<MomentsVO>> listByNew(@RequestParam Long lastId) {
        log.info("查看朋友圈，最后一个帖子的id：{}", lastId);
        List<MomentsVO> momentsVOList = momentsService.listByNew(lastId);
        return Result.success(momentsVOList);
    }

    @Operation(summary = "查看最热门的朋友圈")
    @GetMapping("/list/hot")
    public Result<PageResult<MomentsVO>> listByHot(@RequestParam(defaultValue = "1") int page,
                                                   @RequestParam(defaultValue = "20") int pageSize) {
        log.info("查看第 {} 页的最热帖子", page, pageSize);
        PageResult<MomentsVO> pageResult = momentsService.listByHot(page, pageSize);
        return Result.success(pageResult);
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
    public Result<PageResult<MomentsCommentsVO>> commentReply(@PathVariable("commentId") Long commentId,
                                                              @RequestParam(defaultValue = "1") int page,
                                                              @RequestParam(defaultValue = "5") int pageSize) {
        log.info("查看评论的下级回复列表，commentId: {}, page: {}, pageSize: {}", commentId, page, pageSize);
        PageResult<MomentsCommentsVO> pageResult = momentsService.commentReplies(commentId, page, pageSize);
        return Result.success(pageResult);
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
    @PostMapping("/comment/reply/publish")
    public Result<MomentsCommentsVO> publishCommentReply(@RequestBody MomentCommentsDTO momentCommentsDTO) {
        log.info("发布评论下的回复：{}", momentCommentsDTO);
        MomentsCommentsVO vo = momentsService.publishCommentReply(momentCommentsDTO);
        return Result.success(vo);
    }

    /**
     * 点赞评论
     *
     * @return 点赞评论结果
     */
    @Operation(summary = "点赞评论")
    @PostMapping("/comment/like/{commentId}")
    public Result<Object> likeComment(@PathVariable("commentId") Long commentId) {
        log.info("点赞评论，commentId: {}", commentId);
        momentsService.likeComment(commentId);
        return Result.success();
    }

    /**
     * 查询用户个人帖子信息
     *
     * @return 查询结果
     */
    @Operation(summary = "查询用户个人帖子信息")
    @GetMapping("/my")
    public Result<PageResult<MomentsVO>> queryUserMoments(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        log.info("查询用户个人帖子信息，userId: {}, page: {}, pageSize: {}", page, pageSize);
        PageResult<MomentsVO> pageResult = momentsService.queryUserMoments(page, pageSize);
        return Result.success(pageResult);
    }

    /**
     * 删除帖子
     *
     * @return 删除结果
     */
    @Operation(summary = "删除帖子")
    @DeleteMapping("/delete/{momentId}")
    public Result<Object> delete(@PathVariable Long momentId) {
        log.info("删除帖子，帖子id：{}", momentId);
        momentsService.delete(momentId);
        return Result.success();
    }

    /**
     * 修改帖子
     *
     * @return 修改结果
     */
    @Operation(summary = "修改帖子")
    @PutMapping("/update")
    public Result<Object> update(@RequestBody MomentsDTO momentsDTO) {
        log.info("修改帖子：{}", momentsDTO);
        momentsService.update(momentsDTO);
        return Result.success();
    }

    @Operation(summary = "打赏")
    @PostMapping("/reward")
    public Result<Object> reward(@RequestParam Long momentId, @RequestParam java.math.BigDecimal amount) {
        log.info("打赏，帖子id：{}, 打赏金额: {}", momentId, amount);
        momentsService.reward(momentId, amount);
        return Result.success();
    }

    @Operation(summary = "搜索帖子")
    @GetMapping("/search")
    public Result<PageResult<MomentsVO>> search(@RequestParam String keyword,
                                                @RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int pageSize) {
        log.info("搜索帖子，keyword: {}, page: {}, pageSize: {}", keyword, page, pageSize);
        // PageResult<MomentsVO> pageResult = momentsService.search(keyword, page, pageSize);
        // return Result.success(pageResult);
        return Result.success();
    }

}
