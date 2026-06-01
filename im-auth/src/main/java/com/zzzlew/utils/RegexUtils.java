package com.zzzlew.utils;

import cn.hutool.core.util.StrUtil;
import com.zzzlew.constant.RegexPatterns;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/10 - 11 - 10 - 23:50
 * @Description: com.zzzlew.zzzimserver.utils
 * @version: 1.0
 */
public class RegexUtils {

    /**
     * 校验手机号格式
     *
     * @param phone 手机号
     * @return 是否符合手机号格式
     */
    public static boolean isPhoneInvalid(String phone) {
        return mismatch(phone, RegexPatterns.PHONE_REGEX);
    }

    // 校验是否不符合正则格式
    private static boolean mismatch(String str, String regex) {
        if (StrUtil.isBlank(str)) {
            return true;
        }
        return !str.matches(regex);
    }

}
