package com.zzzlew.server.impl;

import com.zzzlew.domain.dto.MomentsDTO;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.vo.MomentsVO;
import com.zzzlew.mapper.MomentsMapper;
import com.zzzlew.properties.MinIOConfigProperties;
import com.zzzlew.server.MomentsService;
import com.zzzlew.utils.MinIOFileStorgeUtil;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/5 - 06 - 05 - 12:05
 * @Description: com.zzzlew.server.impl
 * @version: 1.0
 */
@Slf4j
@Service
public class MomentsServiceImpl implements MomentsService {

    @Resource
    private MomentsMapper momentsMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private MinIOFileStorgeUtil minIOFileStorgeUtil;
    @Resource
    private MinIOConfigProperties minIOConfigProperties;

    /**
     * 发布朋友圈
     *
     * @param momentsDTO 内容
     */
    @Override
    public void publish(MomentsDTO momentsDTO) {
        UserBaseDTO userBaseDTO = UserHolder.getUser();
        momentsDTO.setUserId(userBaseDTO.getId());
        momentsDTO.setUsername(userBaseDTO.getUsername());
        momentsDTO.setAvatar(userBaseDTO.getAvatar());
        momentsMapper.insert(momentsDTO);
    }

    /**
     * 上传图片
     *
     * @param images 图片
     * @return 图片url
     */
    @Override
    public List<String> uploadImage(List<MultipartFile> images) {
        Long userId = UserHolder.getUser().getId();
        List<String> urlList = new ArrayList<>();

        // 上传图片到minio中
        for (MultipartFile image : images) {
            String imageName = userId + "/" + image.getOriginalFilename();
            String minioUserFavoritePath = minIOFileStorgeUtil.buildFilePath(imageName);
            log.info("minioUserFavoritePath: {}", minioUserFavoritePath);
            minIOFileStorgeUtil.uploadMomentsImage(minioUserFavoritePath, image);
            String url =
                    minIOConfigProperties.getEndpoint() + "/" + minIOConfigProperties.getMomentsBucket() + "/" + minioUserFavoritePath;
            urlList.add(url);
        }

        return urlList;
    }

    /**
     * 查看朋友圈
     *
     * @return 朋友圈列表
     */
    @Override
    public List<MomentsVO> list() {
        List<MomentsVO> momentsVOList = momentsMapper.list();
        return momentsVOList;
    }

}
