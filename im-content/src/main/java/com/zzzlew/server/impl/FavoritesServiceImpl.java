package com.zzzlew.server.impl;

import com.zzzlew.domain.dto.FavoritesDTO;
import com.zzzlew.domain.vo.FavoritesVO;
import com.zzzlew.mapper.FavoritesMapper;
import com.zzzlew.properties.MinIOConfigProperties;
import com.zzzlew.server.FavoritesService;
import com.zzzlew.utils.MinIOFileStorgeUtil;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/8 - 03 - 08 - 15:43
 * @Description: com.zzzlew.server.impl
 * @version: 1.0
 */
@Slf4j
@Service
public class FavoritesServiceImpl implements FavoritesService {

    @Resource
    private MinIOFileStorgeUtil minIOFileStorgeUtil;
    @Resource
    private MinIOConfigProperties minIOConfigProperties;
    @Resource
    private FavoritesMapper favoritesMapper;

    @Override
    public List<String> uploadImage(List<MultipartFile> images) {
        Long userId = UserHolder.getUser().getId();
        List<String> urlList = new ArrayList<>();

        // 上传图片到minio中
        for (MultipartFile image : images) {
            String imageName = userId + "/" + image.getOriginalFilename();
            String minioUserFavoritePath = minIOFileStorgeUtil.buildFilePath(imageName);
            log.info("minioUserFavoritePath: {}", minioUserFavoritePath);
            minIOFileStorgeUtil.uploadFavoriteImage(minioUserFavoritePath, image);
            String url = minIOConfigProperties.getEndpoint() + "/" + minIOConfigProperties.getFavoriteBucket() + "/" + minioUserFavoritePath;
            urlList.add(url);
        }

        return urlList;
    }

    @Override
    public void saveNote(FavoritesDTO favoritesDTO) {
        Long userId = UserHolder.getUser().getId();
        favoritesDTO.setUserId(userId);
        favoritesMapper.saveNote(favoritesDTO);
    }

    @Override
    public void updateNote(FavoritesDTO favoritesDTO) {
        Long userId = UserHolder.getUser().getId();
        favoritesDTO.setUserId(userId);
        favoritesMapper.updateNote(favoritesDTO);
    }

    @Override
    public List<FavoritesVO> getNote() {
        Long userId = UserHolder.getUser().getId();
        List<FavoritesVO> favoritesVOList = favoritesMapper.getNote(userId);
        return favoritesVOList;
    }

    @Override
    public void saveFavorite(FavoritesDTO favoritesDTO) {
        Long userId = UserHolder.getUser().getId();
        favoritesDTO.setUserId(userId);
        log.info("用户 {} 保存收藏：{}", userId, favoritesDTO.getTitle());
        favoritesMapper.saveFavorite(favoritesDTO);
    }

    @Override
    public List<FavoritesVO> getAllFavorites() {
        Long userId = UserHolder.getUser().getId();
        log.info("用户 {} 查询所有收藏", userId);
        return favoritesMapper.getAllFavorites(userId);
    }

    @Override
    public void deleteFavorite(Long id) {
        Long userId = UserHolder.getUser().getId();
        log.info("用户 {} 删除收藏：{}", userId, id);
        favoritesMapper.deleteFavorite(id, userId);
    }

}
