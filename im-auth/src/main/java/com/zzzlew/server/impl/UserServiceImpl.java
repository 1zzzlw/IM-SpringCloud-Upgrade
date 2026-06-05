package com.zzzlew.server.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.RandomUtil;
import com.alibaba.fastjson.JSON;
import com.zzzlew.client.ChatClient;
import com.zzzlew.client.SocialClient;
import com.zzzlew.config.KaptchaConfig;
import com.zzzlew.constant.JwtClaimsConstant;
import com.zzzlew.constant.MessageConstant;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.dto.UserLoginDTO;
import com.zzzlew.domain.dto.UserRegisterDTO;
import com.zzzlew.domain.entity.UserAuth;
import com.zzzlew.domain.entity.UserInfo;
import com.zzzlew.domain.vo.FriendRelationVO;
import com.zzzlew.domain.vo.UserInfoVO;
import com.zzzlew.domain.vo.UserSearchVO;
import com.zzzlew.exception.*;
import com.zzzlew.mapper.UserMapper;
import com.zzzlew.properties.JwtProperties;
import com.zzzlew.properties.MinIOConfigProperties;
import com.zzzlew.result.TokenResult;
import com.zzzlew.server.UserService;
import com.zzzlew.utils.JwtUtil;
import com.zzzlew.utils.MinIOFileStorgeUtil;
import com.zzzlew.utils.RegexUtils;
import com.zzzlew.utils.UserHolder;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static com.zzzlew.constant.RedisConstant.*;


/**
 * @Auther: zzzlew
 * @Date: 2025/11/6 - 11 - 06 - 23:08
 * @Description: com.zzzlew.zzzimserver.server.impl
 * @version: 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final SocialClient socialClient;

    private final ChatClient chatClient;

    @Resource
    private UserMapper userMapper;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Resource
    private JwtProperties jwtproperties;
    @Resource
    private MinIOFileStorgeUtil minIOFileStorgeUtil;
    @Resource
    private MinIOConfigProperties minIOConfigProperties;

    private static final DefaultRedisScript<Long> tokenAtomicScript;

    static {
        // 提前读取lua文件，提高效率
        tokenAtomicScript = new DefaultRedisScript<>();
        // 设置lua脚本文件的位置
        tokenAtomicScript.setLocation(new ClassPathResource("login_token_atomic_operation.lua"));
        // 指定返回值
        tokenAtomicScript.setResultType(Long.class);
    }

    @Override
    public UserInfoVO login(UserLoginDTO userLoginDTO, HttpServletResponse response) {
        String account = userLoginDTO.getAccount();
        String password = userLoginDTO.getPassword();
        String verifyCode = userLoginDTO.getVerifyCode();

        UserInfo userInfo = userMapper.getByAccount(account);

        if (userInfo == null) {
            // 账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        // TODO 密码解密处理
        if (!password.equals(userInfo.getPassword())) {
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        // 从redis中获取验证码，并进行判断
        String code = stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + verifyCode);
        // if (code == null || !code.equals(verifyCode)) {
        //     throw new PasswordErrorException(MessageConstant.VERIFYCODE_ERROR);
        // }

        // 构建用户信息对象
        UserInfoVO userInfoVO = UserInfoVO.builder().id(userInfo.getId()).username(userInfo.getUsername()).avatar(userInfo.getAvatar()).account(userInfo.getAccount()).phone(userInfo.getPhone()).gender(userInfo.getGender()).build();

        // 生成并存储token
        TokenResult tokenResult = generateAndStoreWithUpdateToken(userInfoVO);

        response.setHeader("Authorization", "Bearer " + tokenResult.getAccessToken());
        response.setHeader("refreshtoken", tokenResult.getRefreshToken());

        // 手动将用户信息存入ThreadLocal，以便Feign拦截器使用
        UserBaseDTO userBaseDTO = new UserBaseDTO();
        userBaseDTO.setId(userInfo.getId());
        userBaseDTO.setUsername(userInfo.getUsername());
        userBaseDTO.setAvatar(userInfo.getAvatar());
        userBaseDTO.setPhone(userInfo.getPhone());
        userBaseDTO.setGender(userInfo.getGender());
        UserHolder.save(userBaseDTO);

        try {
            Long userId = userInfo.getId();
            storeFriendListId(userId);
        } finally {
            // 清理ThreadLocal，避免内存泄漏
            UserHolder.removeUser();
        }

        return userInfoVO;
    }

    private void storeFriendListId(Long userId) {
        String friendListKey = USER_FRIEND_LIST_KEY + userId;
        // 登录成功，查询该用户的好友列表，并存入redis中
        List<FriendRelationVO> friendRelationVOList = socialClient.initFriendList(true).getData();
        if (CollectionUtils.isEmpty(friendRelationVOList)) {
            return; // 跳过后续操作，避免空集合传入Redis
        }
        // TODO 暂时先只存好友的id，以后有需要在进行扩展或者改善
        Set<String> friendIdSet = friendRelationVOList.stream().map(vo -> vo.getFriendId().toString()).collect(Collectors.toSet());
        // 将好友列表存储到redis中
        stringRedisTemplate.opsForSet().add(friendListKey, friendIdSet.toArray(new String[0]));
        // 设置好友列表的过期时间
        stringRedisTemplate.expire(friendListKey, USER_FRIEND_LIST_KEY_TTL, TimeUnit.MINUTES);
    }

    /**
     * 处理用户登录界面获取验证码的操作
     *
     * @param response HttpServletResponse对象
     */
    @Override
    public void createCode(HttpServletResponse response) {
        // 生成验证码文本
        String code = KaptchaConfig.kaptchaProducer().createText();
        // 生成验证码图片
        BufferedImage captchaImage = KaptchaConfig.kaptchaProducer().createImage(code);

        String key = LOGIN_CODE_KEY + code;

        // 将验证码信息存入到redis中
        stringRedisTemplate.opsForValue().set(key, code, LOGIN_CODE_TTL, TimeUnit.MINUTES);

        log.info("生成的验证为：{}", code);

        // 设置响应内容类型，告诉浏览器“这次返回的是 JPEG 图片”
        response.setContentType("image/jpeg");

        // 禁止浏览器缓存验证码（避免刷新后还是旧图片）
        response.setHeader("Cache-Control", "no-store, no-cache");
        // 兼容老浏览器
        response.setHeader("Pragma", "no-cache");

        // 获取字节输出流，向客户端发送数据，把 BufferedImage（内存图片）转成二进制流，写进响应
        try (OutputStream os = response.getOutputStream()) {
            // ImageIO.write：把图片写入输出流，格式是 jpeg
            ImageIO.write(captchaImage, "jpeg", os);
            // 确保流里的数据全发送给前端
            os.flush();
        } catch (IOException e) {
            throw new LoginCodeGenerateException("验证码生成错误，请重新刷新");
        }
    }

    /**
     * 处理用户注册时点击注册的操作
     *
     * @param userRegisterDTO 用户注册信息
     */
    @Transactional
    @Override
    public UserAuth register(UserRegisterDTO userRegisterDTO, MultipartFile avatarFile, HttpServletResponse response) {
        // 随机生成一个不会重复的账号
        long timestamp = System.currentTimeMillis();
        String randomStr = RandomUtil.randomString(4);
        int randomNum = new Random().nextInt(90) + 10;
        String account = timestamp + randomStr + randomNum;

        // 随机生成账号
        userRegisterDTO.setAccount(account);

        UserInfo userInfo = BeanUtil.copyProperties(userRegisterDTO, UserInfo.class);

        Long userId = System.currentTimeMillis() * 1000L + RandomUtil.randomInt(1000);

        userInfo.setId(userId);

        // ID/ID.png
        String avatarName = userId + "/" + userId + "." + avatarFile.getContentType().split("/")[1];
        // 生成远端的存储路径 yyyy/m/d/ID/ID.png
        String baseUrl = minIOFileStorgeUtil.buildFilePath(avatarName);
        // 上传用户头像到minio服务端
        minIOFileStorgeUtil.uploadAvatar(baseUrl, avatarFile);
        // 生成本地存储远程路径
        String avatar = minIOConfigProperties.getEndpoint() + "/" + minIOConfigProperties.getAvatarBucket() + "/" + baseUrl;

        userInfo.setAvatar(avatar);

        userMapper.insertUserInfo(userInfo);

        UserInfoVO userInfoVO = BeanUtil.copyProperties(userRegisterDTO, UserInfoVO.class);
        UserAuth userAuth = BeanUtil.copyProperties(userRegisterDTO, UserAuth.class);

        userInfoVO.setId(userId);
        userInfoVO.setAvatar(avatar);
        userAuth.setUserId(userId);
        userAuth.setAvatar(avatar);
        userAuth.setGender(userRegisterDTO.getGender());
        userMapper.insertUserAuth(userAuth);
        log.info("主要用户信息：{}", userAuth);
        // 生成并存储用户登录信息在redis中
        TokenResult tokenResult = generateAndStoreWithUpdateToken(userInfoVO);

        response.setHeader("Authorization", "Bearer " + tokenResult.getAccessToken());
        response.setHeader("refreshtoken", tokenResult.getRefreshToken());

        // 生成ai会话信息
        String conversationId = "ai_" + userId;
        chatClient.createConversation(conversationId, userId, "ai_system", 2);

        return userAuth;
    }

    /**
     * 处理用户注册时点击获取验证码的操作
     *
     * @param phone 手机号
     * @return 验证码
     */
    @Override
    public String createPhoneCode(String phone) {
        // 校验手机号格式
        if (RegexUtils.isPhoneInvalid(phone)) {
            throw new PhoneErrorException(MessageConstant.PHONE_ERROR);
        }

        // 查询数据库，看手机号是否存在
        // TODO 后期可以加上redis缓存，避免频繁查询数据库
        boolean exists = userMapper.getByPhone(phone);
        if (exists) {
            throw new PhoneAlreadyExistsException(MessageConstant.PHONE_ALREADY_EXISTS);
        }

        // 随机生成验证码
        String code = RandomUtil.randomNumbers(6);
        // 将验证码存入到redis中
        String codeKey = REGISTER_CODE_KEY + phone;
        stringRedisTemplate.opsForValue().set(codeKey, code, REGISTER_CODE_TTL, TimeUnit.MINUTES);
        log.info("随机生成的验证码为：{}", code);
        return code;
    }

    /**
     * 处理待登录界面点击确认登录的操作
     *
     * @param token    登录时生成的token
     * @param userId   用户id
     * @param response HttpServletResponse对象
     */
    @Override
    public void pendingLogin(String token, Long userId, HttpServletResponse response) {
        // 判断长期token是否过期
        if (JwtUtil.parseJWT(jwtproperties.getFreshSecretKey(), token) == null) {
            throw new TokenExpiredException(MessageConstant.TOKEN_EXPIRED);
        }
        // 根据用户信息查询一次用户信息
        UserAuth userAuth = userMapper.selectUserInfoById(userId);

        // 构建用户信息对象
        UserInfoVO userInfoVO = UserInfoVO.builder().id(userAuth.getUserId()).username(userAuth.getUsername()).avatar(userAuth.getAvatar()).account(userAuth.getAccount()).phone(userAuth.getPhone()).gender(userAuth.getGender()).build();

        // 校验成功之后，重新生成一个短期token
        TokenResult tokenResult = generateAndStoreWithUpdateToken(userInfoVO);

        response.setHeader("Authorization", "Bearer " + tokenResult.getAccessToken());
        response.setHeader("refreshtoken", tokenResult.getRefreshToken());

        // 手动将用户信息存入ThreadLocal，以便Feign拦截器使用
        UserBaseDTO userBaseDTO = new UserBaseDTO();
        userBaseDTO.setId(userAuth.getUserId());
        userBaseDTO.setUsername(userAuth.getUsername());
        userBaseDTO.setAvatar(userAuth.getAvatar());
        userBaseDTO.setPhone(userAuth.getPhone());
        userBaseDTO.setGender(userAuth.getGender());
        UserHolder.save(userBaseDTO);

        try {
            storeFriendListId(userId);
        } finally {
            // 清理ThreadLocal，避免内存泄漏
            UserHolder.removeUser();
        }
    }

    @Override
    public void refreshToken(Long userId, HttpServletResponse response) {
        // Map<String, Object> claims = new HashMap<>();
        // claims.put(JwtClaimsConstant.USER_ID, userId);
        // String newToken = JwtUtil.createJWT(jwtproperties.getAccessSecretKey(), jwtproperties.getAccessExpiration(),
        //         claims);
        //
        // String userKey = LOGIN_USER_TOKEN_LIST_KEY + userId;
        // // 通过用户id拿到redis中的旧token
        // Set<String> okdTokens = stringRedisTemplate.opsForSet().members(userKey);
        // // 检查是否存在旧token
        // if (okdTokens != null && !okdTokens.isEmpty()) {
        //     for (String oldToken : okdTokens) {
        //         // 遍历旧token列表，看哪个token作为键在redis缓存中有用户信息
        //         String oldTokenKey = LOGIN_USERINFO_ACCESSTOKEN_KEY + oldToken;
        //         Map<Object, Object> userMap = stringRedisTemplate.opsForHash().entries(oldTokenKey);
        //
        //         if (!userMap.isEmpty()) {
        //             // 用户信息不为空，此时这个oldToken中就是上一次使用的token
        //             String newTokenKey = LOGIN_USERINFO_ACCESSTOKEN_KEY + newToken;
        //             stringRedisTemplate.opsForHash().putAll(newTokenKey, userMap);
        //             // 设置过期时间
        //             stringRedisTemplate.expire(newTokenKey, LOGIN_USERINFO_ACCESSTOKEN_KEY_TTL, TimeUnit.MINUTES);
        //             // 删除旧token对应的用户信息
        //             stringRedisTemplate.delete(oldTokenKey);
        //         }
        //     }
        // }
        // // 清空旧的token集合
        // stringRedisTemplate.delete(userKey);
        //
        // // 4. 添加新token到用户token集合
        // stringRedisTemplate.opsForSet().add(userKey, newToken);
        // stringRedisTemplate.expire(userKey, LOGIN_USER_TOKEN_LIST_KEY_TTL, TimeUnit.MINUTES);
        UserBaseDTO userBaseDTO = UserHolder.getUser();
        UserInfoVO userInfoVO = BeanUtil.copyProperties(userBaseDTO, UserInfoVO.class);

        TokenResult tokenResult = generateAndStoreWithUpdateToken(userInfoVO);

        response.setHeader("Authorization", "Bearer " + tokenResult.getAccessToken());
        response.setHeader("refreshtoken", tokenResult.getRefreshToken());
    }

    @Override
    public List<UserAuth> getUserListByIds(List<Long> userIds) {
        return userMapper.selectUserAuthListByUserIdList(userIds);
    }

    // TODO 后期数据库分库，这里的查询需要分段查询
    @Override
    public UserSearchVO search(String number) {
        // 获得当前登录用户的信息，以便查询和好友的状态关系
        Long userId = UserHolder.getUser().getId();

        // 查询用户信息表，根据手机号或账号查询用户信息
        UserSearchVO userSearchVO = userMapper.getByPhoneOrAccount(userId, number);
        // 打印查询到的用户信息
        log.info("userSearchVO: {}", userSearchVO);
        // 检查用户是否存在
        if (userSearchVO == null) {
            log.info("用户不存在");
            return null;
        }
        if (userSearchVO.getIsFriend() == 0) {
            // 不是好友关系
            log.info("不是好友关系");
        } else {
            // 是好友关系
            log.info("是好友关系");
        }
        return userSearchVO;
    }

    // public TokenResult generateAndStoreWithUpdateToken(UserInfoVO userInfoVO) {
    //     // 生成短期token和长期token设置在响应头中
    //     Map<String, Object> claims = new HashMap<>();
    //     Long userId = userInfoVO.getId();
    //     log.info("当前登录用户id：{}", userId);
    //     claims.put(JwtClaimsConstant.USER_ID, userId);
    //     // 生成长期token
    //     String refreshToken =
    //             jwtUtil.createJWT(jwtproperties.getFreshSecretKey(), jwtproperties.getRefreshExpiration(), claims);
    //     // 生成短期token
    //     String accessToken =
    //             jwtUtil.createJWT(jwtproperties.getAccessSecretKey(), jwtproperties.getAccessExpiration(), claims);
    //
    //     String userKey = LOGIN_USER_TOKEN_LIST_KEY + userId;
    //
    //     // 首先检查该用户id下是否有已登录的短期token数据集合，这里就是检测是否是第一次登录，或者是否在很久没有登录，登录信息过期了
    //     Set<String> oldTokens = stringRedisTemplate.opsForSet().members(userKey);
    //
    //     // TODO 后期用上lua脚本处理这里吧，感觉还是有问题，旧的token还是清理不干净
    //
    //     // 如果不为空，需要考虑之前的用户信息是否需要清空，因为token不一样会导致垃圾数据堆积
    //     if (oldTokens != null && !oldTokens.isEmpty()) {
    //         // 将旧Token转换为对应的用户信息key集合
    //         // 遍历删除可能影响效率，考虑批量删除，一次删除多个token对应的用户信息
    //         Collection<String> oldTokenInfoKeys =
    //                 oldTokens.stream().map(oldToken -> LOGIN_USERINFO_KEY + oldToken).collect(Collectors.toList());
    //         Collection<String> oldRefreshTokenInfoKeys = oldTokens.stream()
    //                 .map(oldToken -> LOGIN_USERINFO_REFRESHTOKEN_KEY + oldToken).collect(Collectors.toList());
    //         // 批量删除
    //         stringRedisTemplate.delete(oldTokenInfoKeys);
    //         stringRedisTemplate.delete(oldRefreshTokenInfoKeys);
    //         // 再删除这个Set集合本身
    //         stringRedisTemplate.delete(userKey);
    //     }
    //
    //     // 存储userId为键的所有用户的token信息
    //     stringRedisTemplate.opsForSet().add(userKey, accessToken);
    //     stringRedisTemplate.opsForSet().add(userKey, refreshToken);
    //     // 设置有效期
    //     stringRedisTemplate.expire(userKey, LOGIN_USER_TOKEN_LIST_KEY_TTL, TimeUnit.MINUTES);
    //
    //     log.info("存储用户信息：{}", userInfoVO);
    //     // 将用户信息转为map集合
    //     Map<String, Object> map = BeanUtil.beanToMap(userInfoVO, new HashMap<>(), CopyOptions.create()
    //             .setIgnoreNullValue(true).setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));
    //
    //     // redis中键的格式为：login:user:token
    //     String tokenKey = LOGIN_USERINFO_KEY + accessToken;
    //     String refreshTokenKey = LOGIN_USERINFO_REFRESHTOKEN_KEY + refreshToken;
    //     // 将用户信息存储到redis中
    //     stringRedisTemplate.opsForHash().putAll(tokenKey, map);
    //     // 设置有效期
    //     stringRedisTemplate.expire(tokenKey, LOGIN_USERINFO_KEY_TTL, TimeUnit.MINUTES);
    //
    //     stringRedisTemplate.opsForHash().putAll(refreshTokenKey, map);
    //     stringRedisTemplate.expire(refreshTokenKey, LOGIN_USERINFO_REFRESHTOKEN_KEY_TTL, TimeUnit.MINUTES);
    //
    //     TokenResult tokenResult = new TokenResult();
    //     tokenResult.setAccessToken(accessToken);
    //     tokenResult.setRefreshToken(refreshToken);
    //
    //     return tokenResult;
    // }

    public TokenResult generateAndStoreWithUpdateToken(UserInfoVO userInfoVO) {
        // 生成短期token和长期token设置在响应头中
        Map<String, Object> claims = new HashMap<>();
        Long userId = userInfoVO.getId();
        log.info("当前登录用户id：{}，用户信息：{}", userId, userInfoVO);
        claims.put(JwtClaimsConstant.USER_ID, userId);
        claims.put(JwtClaimsConstant.USERNAME, userInfoVO.getUsername());
        claims.put(JwtClaimsConstant.ACCOUNT, userInfoVO.getAccount());
        claims.put(JwtClaimsConstant.AVATAR, userInfoVO.getAvatar());
        claims.put(JwtClaimsConstant.GENDER, userInfoVO.getGender());
        claims.put(JwtClaimsConstant.PHONE, userInfoVO.getPhone());
        // 生成长期token
        String refreshToken = JwtUtil.createJWT(jwtproperties.getFreshSecretKey(), jwtproperties.getRefreshExpiration(), claims);
        // 生成短期token
        String accessToken = JwtUtil.createJWT(jwtproperties.getAccessSecretKey(), jwtproperties.getAccessExpiration(), claims);

        String userKey = LOGIN_USER_TOKEN_LIST_KEY + userId;
        String accessTokenKey = LOGIN_USERINFO_ACCESSTOKEN_KEY + accessToken;
        String refreshTokenKey = LOGIN_USERINFO_REFRESHTOKEN_KEY + refreshToken;

        List<String> keys = Arrays.asList(userKey, accessTokenKey, refreshTokenKey);

        String userInfoJson = JSON.toJSONString(userInfoVO);

        log.info("用户信息的json数据: {}", userInfoJson);

        List<String> args = Arrays.asList(accessToken, refreshToken, LOGIN_USER_TOKEN_LIST_KEY_TTL.toString(), LOGIN_USERINFO_ACCESSTOKEN_KEY_TTL.toString(), LOGIN_USERINFO_REFRESHTOKEN_KEY_TTL.toString(), userInfoJson);

        // 执行Lua脚本（原子化完成：清理旧Token + 存储新Token + 写入用户信息）
        Long executeResult = stringRedisTemplate.execute(tokenAtomicScript, keys, args.toArray(new String[0]));

        // 脚本执行结果校验
        if (executeResult == null || executeResult != 1) {
            log.error("用户{}的Token原子化操作失败", userId);
            throw new TokenGenerateException("Token生成与存储失败");
        }

        // 6. 封装返回结果
        TokenResult tokenResult = new TokenResult();
        tokenResult.setAccessToken(accessToken);
        tokenResult.setRefreshToken(refreshToken);
        return tokenResult;
    }

}
