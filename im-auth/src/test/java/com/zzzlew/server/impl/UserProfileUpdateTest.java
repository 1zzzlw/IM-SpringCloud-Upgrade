package com.zzzlew.server.impl;

import com.zzzlew.client.ChatClient;
import com.zzzlew.client.PayClient;
import com.zzzlew.client.SocialClient;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.domain.dto.UserProfileUpdateDTO;
import com.zzzlew.domain.entity.UserInfo;
import com.zzzlew.exception.BaseException;
import com.zzzlew.mapper.UserMapper;
import com.zzzlew.utils.UserHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class UserProfileUpdateTest {
    private UserMapper mapper;
    private UserServiceImpl service;
    private UserInfo current;

    @BeforeEach
    void setup() {
        mapper = mock(UserMapper.class);
        service = new UserServiceImpl(mock(SocialClient.class), mock(ChatClient.class), mock(PayClient.class));
        ReflectionTestUtils.setField(service, "userMapper", mapper);
        UserBaseDTO identity = new UserBaseDTO();
        identity.setId(42L);
        UserHolder.save(identity);
        current = new UserInfo();
        current.setId(42L);
        current.setUsername("原昵称");
        current.setAccount("my_account");
        current.setGender(1);
        current.setAddress("原地址");
        when(mapper.selectProfileById(42L)).thenReturn(current);
    }

    @AfterEach
    void cleanup() { UserHolder.removeUser(); }

    private UserProfileUpdateDTO valid() {
        UserProfileUpdateDTO dto = new UserProfileUpdateDTO();
        dto.setAccount("my_account");
        dto.setUsername("新的昵称");
        dto.setGender(0);
        dto.setAddress("杭州");
        dto.setEmail("test@example.test");
        dto.setBirthday(LocalDate.of(2000, 1, 1));
        return dto;
    }

    @Test
    void updatesDetailsAndAuthProjectionForAuthenticatedUser() {
        UserProfileUpdateDTO dto = valid();
        service.updateProfile(dto);
        verify(mapper).updateProfile(42L, "my_account", "test@example.test", LocalDate.of(2000, 1, 1), "新的昵称", 0, "杭州");
        verify(mapper).updateAuthProfile(42L, "my_account", "新的昵称", 0);
    }

    @Test
    void rejectsInvalidNewFieldsBeforeWriting() {
        UserProfileUpdateDTO dto = valid();
        dto.setUsername(" ");
        assertThrows(BaseException.class, () -> service.updateProfile(dto));
        dto.setUsername("名".repeat(33));
        assertThrows(BaseException.class, () -> service.updateProfile(dto));
        dto.setUsername("有效昵称");
        dto.setGender(2);
        assertThrows(BaseException.class, () -> service.updateProfile(dto));
        dto.setGender(1);
        dto.setAddress("址".repeat(256));
        assertThrows(BaseException.class, () -> service.updateProfile(dto));
        verify(mapper, never()).updateAuthProfile(anyLong(), anyString(), anyString(), any());
        verify(mapper, never()).updateProfile(anyLong(), anyString(), any(), any(), anyString(), any(), any());
    }

    @Test
    void retainsDetailsWhenOlderClientOmitsNewFieldsAndAllowsClearingAddress() {
        UserProfileUpdateDTO dto = valid();
        dto.setUsername(null);
        dto.setGender(null);
        dto.setAddress(null);
        service.updateProfile(dto);
        verify(mapper).updateProfile(42L, "my_account", "test@example.test", dto.getBirthday(), "原昵称", 1, "原地址");
        dto.setAddress("  ");
        service.updateProfile(dto);
        verify(mapper).updateProfile(42L, "my_account", "test@example.test", dto.getBirthday(), "原昵称", 1, "");
    }
}
