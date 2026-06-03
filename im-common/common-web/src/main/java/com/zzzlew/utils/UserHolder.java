package com.zzzlew.utils;

import com.zzzlew.domain.dto.UserBaseDTO;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/12 - 11 - 12 - 0:08
 * @Description: com.zzzlew.zzzimserver.utils
 * @version: 1.0
 */
public class UserHolder {
    private static final ThreadLocal<UserBaseDTO> threadLocal = new ThreadLocal<>();

    public static void save(UserBaseDTO userBaseDTO) {
        threadLocal.set(userBaseDTO);
    }

    public static UserBaseDTO getUser() {
        return threadLocal.get();
    }

    public static void removeUser() {
        threadLocal.remove();
    }
}
