package com.zzzlew.exception;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/6 - 11 - 06 - 23:43
 * @Description: com.zzzlew.zzzimserver.exception
 * @version: 1.0
 */

/**
 * 业务异常
 */
public class BaseException extends RuntimeException {
    public BaseException() {
    }

    public BaseException(String msg) {
        super(msg);
    }
}
