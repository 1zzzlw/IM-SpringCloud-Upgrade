package com.zzzlew.mapper;

import com.github.pagehelper.Page;
import com.zzzlew.domain.dto.MomentCommentsPageQueryDTO;
import com.zzzlew.domain.dto.MomentsDTO;
import com.zzzlew.domain.vo.MomentsCommentsVO;
import com.zzzlew.domain.vo.MomentsVO;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/5 - 06 - 05 - 23:34
 * @Description: com.zzzlew.mapper
 * @version: 1.0
 */
public interface MomentsMapper {

    /**
     * 插入朋友圈
     *
     * @param momentsDTO 朋友圈信息
     */
    void insert(MomentsDTO momentsDTO);

    /**
     * 查询朋友圈
     *
     * @param lastId  最后数据id
     * @return 朋友圈列表
     */
    List<MomentsVO> listByNew(Long lastId, int pageSize);

    /**
     * 点赞
     *
     * @param momentId 朋友圈id
     * @param i        操作类型
     */
    void like(Long momentId, int i);

    /**
     * 更新评论计数
     *
     * @param momentId 朋友圈id
     * @param increment 增量（+1 或 -1）
     */
    void updateCommentCount(Long momentId, int increment);

    /**
     * 根据id查询朋友圈
     *
     * @param momentId 朋友圈id
     * @return 朋友圈信息
     */
    MomentsVO getById(Long momentId);

    /**
     * 根据ids批量查询朋友圈
     *
     * @param missingIds 缺少的id
     * @return 朋友圈信息
     */
    List<MomentsVO> selectByIds(List<Long> missingIds);

    /**
     * 评论
     */
    void publishComment(MomentsCommentsVO momentsCommentsVO);

    /**
     * 发布评论下的回复
     */
    void publishCommentReply(MomentsCommentsVO reply);

    /**
     * 获取评论
     *
     * @return 朋友圈评论列表
     */
    Page<MomentsCommentsVO> comments(MomentCommentsPageQueryDTO queryDTO);

    /**
     * 获取评论的下级回复列表
     *
     * @param commentId 评论ID
     * @return 回复列表
     */
    Page<MomentsCommentsVO> commentReplies(Long commentId);

    /**
     * 点赞评论
     *
     * @param commentId 评论ID
     * @param increment 增量（+1 或 -1）
     */
    void likeComment(Long commentId, int increment);

    /**
     * 获取最热朋友圈
     *
     * @return 最热朋友圈列表
     */
    Page<MomentsVO> listByHot();

}
