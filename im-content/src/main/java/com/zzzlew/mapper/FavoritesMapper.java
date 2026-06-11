package com.zzzlew.mapper;

import com.zzzlew.domain.dto.FavoritesDTO;
import com.zzzlew.domain.vo.FavoritesVO;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/8 - 03 - 08 - 16:50
 * @Description: com.zzzlew.mapper
 * @version: 1.0
 */
public interface FavoritesMapper {


    /**
     * 保存笔记
     *
     * @param favoritesDTO
     */
    void saveNote(FavoritesDTO favoritesDTO);

    /**
     * 更新笔记
     *
     * @param favoritesDTO
     */
    void updateNote(FavoritesDTO favoritesDTO);

    /**
     * 获取笔记
     *
     * @param userId
     * @return
     */
    List<FavoritesVO> getNote(Long userId);

    /**
     * 保存收藏（支持所有类型：文本、图片、视频、文件等）
     *
     * @param favoritesDTO 收藏信息
     */
    void saveFavorite(FavoritesDTO favoritesDTO);

    /**
     * 获取当前用户所有收藏（包含所有类型）
     *
     * @param userId 用户ID
     * @return 收藏列表
     */
    List<FavoritesVO> getAllFavorites(Long userId);

    /**
     * 删除收藏
     *
     * @param id     收藏ID
     * @param userId 用户ID（防止越权删除）
     */
    void deleteFavorite(@Param("id") Long id, @Param("userId") Long userId);
}
