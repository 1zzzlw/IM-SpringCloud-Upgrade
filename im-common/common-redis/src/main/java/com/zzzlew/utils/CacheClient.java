package com.zzzlew.utils;

import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/9 - 06 - 09 - 20:39
 * @Description: com.zzzlew.utils
 * @version: 1.0
 */
@Slf4j
@Component
public class CacheClient {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


}
