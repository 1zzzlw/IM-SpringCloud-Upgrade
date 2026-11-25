package com.zzzlew.controller;

import com.zzzlew.domain.vo.UserInfoVO;
import com.zzzlew.exception.TokenExpiredException;
import com.zzzlew.server.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SessionLoginControllerTest {
    private UserService service;
    private MockMvc mvc;

    @BeforeEach
    void setup() {
        service = mock(UserService.class);
        UserController controller = new UserController();
        ReflectionTestUtils.setField(controller, "userService", service);
        mvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void acceptsRefreshTokenInHeaderAndReturnsCurrentProfile() throws Exception {
        when(service.pendingLogin(eq("fixture-refresh"), eq(1792113130553811123L), eq("desktop_test"), any())).thenReturn(UserInfoVO.builder().id(1792113130553811123L).account("current-account").build());
        mvc.perform(post("/user/pendingLogin").header("Authorization", "Bearer fixture-refresh").header("X-Device-Id", "desktop_test").contentType("application/json").content("{\"userId\":\"1792113130553811123\"}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.code").value(1)).andExpect(jsonPath("$.data.id").value("1792113130553811123")).andExpect(jsonPath("$.data.account").value("current-account"));
    }

    @Test
    void expiredSessionIsUnauthorizedAndDoesNotReturnCredentials() throws Exception {
        when(service.pendingLogin(anyString(), anyLong(), isNull(), any())).thenThrow(new TokenExpiredException("登录已失效"));
        mvc.perform(post("/user/pendingLogin").header("Authorization", "expired").contentType("application/json").content("{\"userId\":42}"))
            .andExpect(status().isUnauthorized()).andExpect(header().doesNotExist("Authorization")).andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void oldGetEndpointCannotSubmitCredentialsInUrl() throws Exception {
        mvc.perform(get("/user/pendingLogin").param("token", "never-log").param("userId", "42"))
            .andExpect(status().isMethodNotAllowed());
        verifyNoInteractions(service);
    }
}
