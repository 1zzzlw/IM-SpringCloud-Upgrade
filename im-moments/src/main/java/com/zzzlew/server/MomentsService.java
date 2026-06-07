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
     * @param sortWay 排序方式
     * @param lastId   最后一个朋友圈的id
     * @return 查看结果
     */
    List<MomentsVO> list(Integer sortWay, Long lastId);

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
}
