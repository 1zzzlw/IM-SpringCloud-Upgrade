package com.zzzlew.config;

import cn.hutool.json.JSONUtil;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.utils.UserHolder;
import feign.Logger;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Bean;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/3 - 06 - 03 - 21:37
 * @Description: com.zzzlew.config
 * @version: 1.0
 */
public class DefaultFeignConfig {
    @Bean
    public Logger.Level feignLogLevel() {
        return Logger.Level.FULL;
    }

    @Bean
    public RequestInterceptor userInfoRequestInterceptor() {
        return new RequestInterceptor() {
            @Override
            public void apply(RequestTemplate template) {
                // 获取登录用户
                UserBaseDTO userBaseDTO = UserHolder.getUser();
                if (userBaseDTO == null) {
                    // 如果为空则直接跳过
                    return;
                }
                String userInfoJson = JSONUtil.toJsonStr(userBaseDTO);
                // 如果不为空则放入请求头中，传递给下游微服务
                template.header("user-info", userInfoJson);
            }
        };
    }
}
