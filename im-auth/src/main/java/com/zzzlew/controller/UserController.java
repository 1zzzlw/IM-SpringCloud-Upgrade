package com.zzzlew.controller;


import com.zzzlew.annotaion.UrlLimit;
import com.zzzlew.domain.dto.UserLoginDTO;
import com.zzzlew.domain.dto.UserRegisterDTO;
import com.zzzlew.domain.entity.UserAuth;
import com.zzzlew.domain.vo.UserInfoVO;
import com.zzzlew.domain.vo.UserSearchVO;
import com.zzzlew.enums.LimitKeyType;
import com.zzzlew.result.Result;
import com.zzzlew.server.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/6 - 11 - 06 - 23:07
 * @Description: com.zzzlew.zzzimserver.controller
 * @version: 1.0
 */
@Slf4j
@RestController
@RequestMapping("/user")
@Tag(name = "用户模块")
public class UserController {

    @Resource
    private UserService userService;

    /**
     * 用户登录
     *
     * @param userLoginDTO 用户登录信息
     * @return 用户登录vo
     */
    @Operation(summary = "用户登录")
    @PostMapping("/login")
    @UrlLimit(keyType = LimitKeyType.IP)
    public Result<UserInfoVO> login(@RequestBody UserLoginDTO userLoginDTO, HttpServletResponse response) {
        log.info("当前登录用户信息：{}", userLoginDTO);
        UserInfoVO userInfoVO = userService.login(userLoginDTO, response);
        log.info("登录成功，当前登录用户信息：{}", userInfoVO);
        return Result.success(userInfoVO);
    }

    /**
     * 生成登录验证码
     *
     * @param response HttpServletResponse
     */
    @Operation(summary = "生成登录验证码")
    @GetMapping("/verifyCode")
    public void verifyCode(HttpServletResponse response) {
        log.info("随机生成登录验证码");
        userService.createCode(response);
    }

    /**
     * 用户登录确认
     *
     * @param token  登录凭证
     * @param userId 用户id
     * @return 登录确认结果
     */
    @Operation(summary = "用户登录确认")
    @GetMapping("/pendingLogin")
    public Result<Object> pendingLogin(@RequestParam("token") String token, @RequestParam("userId") Long userId, HttpServletResponse response) {
        log.info("用户 {} 正在登录，用户id为 {}", token, userId);
        userService.pendingLogin(token, userId, response);
        return Result.success();
    }

    /**
     * 用户注册
     *
     * @param userRegisterDTO 用户注册信息
     * @return 注册成功后的token
     */
    @Operation(summary = "用户注册")
    @PostMapping("/register")
    public Result<UserAuth> register(UserRegisterDTO userRegisterDTO, @RequestParam(value = "avatarFile") MultipartFile avatarFile, HttpServletResponse response) {
        log.info("注册用户信息为 {}，头像信息为 {}", userRegisterDTO, avatarFile);
        UserAuth userAuth = userService.register(userRegisterDTO, avatarFile, response);
        return Result.success(userAuth);
    }

    /**
     * 创建手机号验证码
     *
     * @param phone 手机号
     * @return 验证码
     */
    @Operation(summary = "创建手机号验证码")
    @PostMapping("/phoneCode")
    public Result<String> createCode(@RequestParam("phone") String phone) {
        log.info("创建手机号 {} 的验证码", phone);
        String phoneCode = userService.createPhoneCode(phone);
        return Result.success(phoneCode);
    }

    /**
     * 批量获取用户信息
     *
     * @param userIds 用户id列表
     * @return 用户信息列表
     */
    @PostMapping("/list/ids")
    public Result<List<UserAuth>> getUserListByIds(@RequestBody List<Long> userIds) {
        List<UserAuth> userAuths = userService.getUserListByIds(userIds);
        return Result.success(userAuths);
    }

    /**
     * 搜索用户
     *
     * @param number 手机号 / 账号
     * @return 用户搜索vo
     */
    @Operation(summary = "搜索用户")
    @GetMapping("/search")
    public Result<UserSearchVO> search(String number) {
        log.info("搜索用户 {} 的信息", number);
        UserSearchVO userSearchVO = userService.search(number);
        return Result.success(userSearchVO);
    }

}
