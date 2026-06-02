package com.zzzlew.mapper;

import com.zzzlew.domain.dto.FavoritesDTO;
import com.zzzlew.domain.vo.FavoritesVO;

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
}
