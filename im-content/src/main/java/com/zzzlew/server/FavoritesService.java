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
     * @return
     */
    List<FavoritesVO> getNote();

}
