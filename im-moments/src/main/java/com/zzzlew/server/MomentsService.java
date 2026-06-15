package com.zzzlew.server;

import com.zzzlew.domain.dto.MomentCommentsDTO;
import com.zzzlew.domain.dto.MomentCommentsPageQueryDTO;
import com.zzzlew.domain.dto.MomentsDTO;
import com.zzzlew.domain.vo.MomentsCommentsVO;
import com.zzzlew.domain.vo.MomentsVO;
import com.zzzlew.result.PageResult;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/5 - 06 - 05 - 12:04
 * @Description: com.zzzlew.server
 * @version: 1.0
 */
public interface MomentsService {
    /**
     * 发布朋友圈
     *
     * @param momentsDTO 朋友圈内容
     */
    void publish(MomentsDTO momentsDTO);

    /**
     * 上传图片
     *
     * @param images 图片
     * @return 图片地址
     */
    List<String> uploadImage(List<MultipartFile> images);

    /**
     * 查看朋友圈
     *
     * @param lastId  最后一个朋友圈的id
     * @return 查看结果
     */
    List<MomentsVO> listByNew(Long lastId);

    /**
     * 查看最热门的朋友圈
     *
     * @param page     页码
     * @param pageSize 每页显示记录数
     * @return 查看结果
     */
    PageResult<MomentsVO> listByHot(int page, int pageSize);

    /**
     * 点赞
     *
     * @param momentId 朋友圈id
     */
    void like(Long momentId);

    /**
     * 获取朋友圈
     *
     * @param momentId 朋友圈id
     * @return 朋友圈
     */
    MomentsVO getById(Long momentId);

    /**
     * 发布评论
     *
     * @param momentCommentsDTO 评论内容
     * @return 发布结果
     */
    MomentsCommentsVO publishComment(MomentCommentsDTO momentCommentsDTO);

    /**
     * 查看评论
     *
     * @return 评论结果
     */
    PageResult<MomentsCommentsVO> comments(MomentCommentsPageQueryDTO momentCommentsPageQueryDTO);

    /**
     * 查看评论的下级回复列表
     *
     * @param commentId 评论ID
     * @param page      页码
     * @param pageSize  每页大小
     * @return 回复列表
     */
    PageResult<MomentsCommentsVO> commentReplies(Long commentId, int page, int pageSize);

    /**
     * 发布评论下的回复
     *
     * @param momentCommentsDTO 回复内容
     * @return 回复信息
     */
    MomentsCommentsVO publishCommentReply(MomentCommentsDTO momentCommentsDTO);

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     */
    void likeComment(Long commentId);

    /**
     * 查询用户朋友圈
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @return 朋友圈列表
     */
    PageResult<MomentsVO> queryUserMoments(int page, int pageSize);

    /**
     * 删除朋友圈
     *
     * @param momentId 朋友圈ID
     */
    void delete(Long momentId);
}
