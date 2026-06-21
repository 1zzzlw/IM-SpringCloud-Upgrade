package com.zzzlew.server;

import com.zzzlew.domain.dto.FavoritesDTO;
import com.zzzlew.domain.vo.FavoritesVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/8 - 03 - 08 - 15:43
 * @Description: com.zzzlew.server
 * @version: 1.0
 */
public interface FavoritesService {

    /**
     * 上传图片
     *
     * @param images
     */
    List<String> uploadImage(List<MultipartFile> images);

    /**
     * 保存笔记
     *
     * @param favoritesDTO
     * @return 生成的笔记ID
     */
    Long saveNote(FavoritesDTO favoritesDTO);

    /**
     * 更新笔记
     *
     * @param favoritesDTO
     */
    void updateNote(FavoritesDTO favoritesDTO);

    /**
     * 获取笔记
     *
     * @return
     */
    List<FavoritesVO> getNote();

    /**
     * 保存收藏（支持所有类型）
     *
     * @param favoritesDTO 收藏信息
     */
    void saveFavorite(FavoritesDTO favoritesDTO);

    /**
     * 获取当前用户所有收藏
     *
     * @return 收藏列表
     */
    List<FavoritesVO> getAllFavorites();

    /**
     * 删除收藏
     *
     * @param id 收藏ID
     */
    void deleteFavorite(Long id);

}
