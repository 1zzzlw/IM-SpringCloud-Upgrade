package com.zzzlew.server;

import com.zzzlew.domain.dto.MomentsDTO;
import com.zzzlew.domain.vo.MomentsVO;
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
     * @return 查看结果
     */
    List<MomentsVO> list();

}
