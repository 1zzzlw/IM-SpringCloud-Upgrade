package com.zzzlew.annotaion;

import com.zzzlew.enums.LimitKeyType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @Auther: zzzlew
 * @Date: 2026/4/6 - 04 - 06 - 16:56
 * @Description: com.zzzlew.annotaion
 * @version: 1.0
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UrlLimit {
    LimitKeyType keyType() default LimitKeyType.ID; // 限制类型，ip或者id

    int maxRequests() default 30; // 每分钟最大请求次数
}
