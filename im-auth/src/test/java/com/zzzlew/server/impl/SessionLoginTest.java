package com.zzzlew.server.impl;

import com.zzzlew.client.ChatClient;
import com.zzzlew.client.PayClient;
import com.zzzlew.client.SocialClient;
import com.zzzlew.constant.JwtClaimsConstant;
import com.zzzlew.domain.entity.UserAuth;
import com.zzzlew.domain.vo.UserInfoVO;
import com.zzzlew.exception.TokenExpiredException;
import com.zzzlew.mapper.UserMapper;
import com.zzzlew.properties.JwtProperties;
import com.zzzlew.result.Result;
import com.zzzlew.utils.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.Map;
import java.util.HashMap;

import static com.zzzlew.constant.RedisConstant.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SessionLoginTest {
    private UserServiceImpl service;
    private UserMapper mapper;
    private HashOperations<String, Object, Object> hashes;
    private MockHttpServletResponse response;
    private final String secret = "session-test-only-secret-123456789";

    @BeforeEach
    void setup() {
        SocialClient social = mock(SocialClient.class);
        when(social.initFriendList(true)).thenReturn(Result.success(Collections.emptyList()));
        service = new UserServiceImpl(social, mock(ChatClient.class), mock(PayClient.class));
        mapper = mock(UserMapper.class);
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        hashes = mock(HashOperations.class);
        when(redis.opsForHash()).thenReturn(hashes);
        JwtProperties properties = new JwtProperties();
        properties.setFreshSecretKey(secret);
        properties.setAccessSecretKey(secret);
        properties.setRefreshExpiration(60000L);
        properties.setAccessExpiration(60000L);
        ReflectionTestUtils.setField(service, "userMapper", mapper);
        ReflectionTestUtils.setField(service, "stringRedisTemplate", redis);
        ReflectionTestUtils.setField(service, "jwtproperties", properties);
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(1L);
        response = new MockHttpServletResponse();
    }

    private String token(long userId, long duration) {
        return JwtUtil.createJWT(secret, duration, new HashMap<>(Map.of(JwtClaimsConstant.USER_ID, userId)));
    }

    @Test
    void rejectsTokenForAnotherAccountBeforeReadingProfile() {
        assertThrows(TokenExpiredException.class, () -> service.pendingLogin(token(1L, 60000), 2L, null, response));
        verifyNoInteractions(mapper);
        verify(hashes, never()).entries(any());
    }

    @Test
    void rejectsExpiredOrMalformedToken() {
        assertThrows(TokenExpiredException.class, () -> service.pendingLogin(token(1L, -60000), 1L, null, response));
        assertThrows(TokenExpiredException.class, () -> service.pendingLogin("invalid", 1L, null, response));
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsRevokedSessionAndMismatchingCachedIdentity() {
        String token = token(1L, 60000);
        when(hashes.entries(LOGIN_USERINFO_REFRESHTOKEN_KEY + token)).thenReturn(Collections.emptyMap());
        assertThrows(TokenExpiredException.class, () -> service.pendingLogin(token, 1L, null, response));
        when(hashes.entries(LOGIN_USERINFO_REFRESHTOKEN_KEY + token)).thenReturn(Map.of("id", "2"));
        assertThrows(TokenExpiredException.class, () -> service.pendingLogin(token, 1L, null, response));
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsQuickLoginForAnotherDeviceBeforeReadingProfile() {
        String token = JwtUtil.createJWT(secret, 60000, new HashMap<>(Map.of(
                JwtClaimsConstant.USER_ID, 1L, DEVICE_ID_CLAIM, "app_one", SESSION_ID_CLAIM, "session-one")));
        when(hashes.entries(LOGIN_USERINFO_REFRESHTOKEN_KEY + token)).thenReturn(Map.of("id", "1"));
        assertThrows(TokenExpiredException.class, () -> service.pendingLogin(token, 1L, "desktop_one", response));
        verifyNoInteractions(mapper);
    }

    @Test
    void validSessionReturnsFreshProfileAndRotatesBothTokens() {
        String token = token(1L, 60000);
        when(hashes.entries(LOGIN_USERINFO_REFRESHTOKEN_KEY + token)).thenReturn(Map.of("id", "1"));
        UserAuth user = new UserAuth();
        user.setUserId(1L);
        user.setAccount("new-account");
        user.setUsername("new-name");
        when(mapper.selectUserInfoById(1L)).thenReturn(user);
        UserInfoVO profile = service.pendingLogin(token, 1L, null, response);
        assertEquals("new-account", profile.getAccount());
        assertEquals("new-name", profile.getUsername());
        assertNotNull(response.getHeader("Authorization"));
        String rotated = response.getHeader("refreshtoken");
        assertNotEquals(token, rotated);
        var claims = JwtUtil.parseJWT(secret, rotated);
        assertEquals(LEGACY_DEVICE_ID, claims.get(DEVICE_ID_CLAIM));
        assertNotNull(claims.get(SESSION_ID_CLAIM));
    }
}
