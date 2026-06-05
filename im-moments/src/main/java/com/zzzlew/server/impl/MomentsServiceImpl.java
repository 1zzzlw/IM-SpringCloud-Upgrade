package com.zzzlew.server.impl;

import com.zzzlew.domain.dto.MomentsDTO;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.mapper.MomentsMapper;
import com.zzzlew.server.MomentsService;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

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

    /**
     * 发布朋友圈
     *
     * @param content 内容
     */
    @Override
    public void publish(String content) {
        UserBaseDTO userBaseDTO = UserHolder.getUser();
        MomentsDTO momentsDTO = new MomentsDTO();
        momentsDTO.setUserId(userBaseDTO.getId());
        momentsDTO.setUsername(userBaseDTO.getUsername());
        momentsDTO.setAvatar(userBaseDTO.getAvatar());
        momentsDTO.setContent(content);
        momentsMapper.insert(momentsDTO);
    }

}
