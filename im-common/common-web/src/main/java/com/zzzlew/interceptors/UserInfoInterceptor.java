package com.zzzlew.interceptors;

import com.alibaba.fastjson.JSON;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.Nullable;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/3 - 06 - 03 - 16:09
 * @Description: com.zzzlew.interceptors
 * @version: 1.0
 */
@Slf4j
public class UserInfoInterceptor implements HandlerInterceptor {
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取请求头中的用户信息
        String userInfo = request.getHeader("user-info");
        log.info("存入用户信息：{} 到上下文中", userInfo);
        // 解析JSON格式的用户信息为对象
        UserBaseDTO userBaseDTO = JSON.parseObject(userInfo, UserBaseDTO.class);
        if (userBaseDTO != null) {
            // 将用户信息存储到ThreadLocal中
            UserHolder.save(userBaseDTO);
        }

        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, @Nullable Exception ex) throws Exception {
        // 从ThreadLocal中移除用户信息
        UserHolder.removeUser();
    }
}
