package com.zzzlew.server;

import com.zzzlew.domain.dto.UserLoginDTO;
import com.zzzlew.domain.dto.UserRegisterDTO;
import com.zzzlew.domain.entity.UserAuth;
import com.zzzlew.domain.vo.UserInfoVO;
import com.zzzlew.domain.vo.UserSearchVO;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/6 - 11 - 06 - 23:08
 * @Description: com.zzzlew.zzzimserver.server
 * @version: 1.0
 */
public interface UserService {

    /**
     * 用户登录
     *
     * @param userLoginDTO 用户登录DTO
     * @return 用户信息VO
     */
    UserInfoVO login(UserLoginDTO userLoginDTO, HttpServletResponse response);

    /**
     * 创建验证码
     *
     * @param response HttpServletResponse
     */
    void createCode(HttpServletResponse response);

    /**
     * 用户注册
     *
     * @param userRegisterDTO 用户注册DTO
     * @return 用户id
     */
    UserAuth register(UserRegisterDTO userRegisterDTO, MultipartFile avatarFile, HttpServletResponse response);

    /**
     * 创建验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    String createPhoneCode(String phone);

    /**
     * 用户登录确认
     *
     * @param token  登录时生成的token
     * @param userId 用户id
     */
    void pendingLogin(String token, Long userId, HttpServletResponse response);

    /**
     * 刷新token
     *
     * @param userId 用户id
     */
    void refreshToken(Long userId, HttpServletResponse response);

    /**
     * 根据用户id列表获取用户信息
     *
     * @param userIds 用户id列表
     * @return
     */
    List<UserAuth> getUserListByIds(List<Long> userIds);

    /**
     * 搜索用户
     *
     * @param number 手机号 / 账号
     * @return 用户搜索VO
     */
    UserSearchVO search(String number);
}
