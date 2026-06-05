package com.zzzlew.mapper;

import com.zzzlew.domain.dto.MomentsDTO;

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

}
