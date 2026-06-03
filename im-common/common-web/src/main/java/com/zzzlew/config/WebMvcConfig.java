package com.zzzlew.config;

import com.zzzlew.interceptors.UserInfoInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/12 - 11 - 12 - 17:15
 * @Description: com.zzzlew.zzzimserver.config
 * @version: 1.0
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 刷新Redis缓存拦截器，主要拦截短期token
        registry.addInterceptor(new UserInfoInterceptor()).addPathPatterns("/**")
                .order(0);
    }

}
