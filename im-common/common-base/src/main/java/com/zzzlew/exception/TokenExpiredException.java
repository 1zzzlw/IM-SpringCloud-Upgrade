package com.zzzlew.exception;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/17 - 11 - 17 - 19:15
 * @Description: com.zzzlew.zzzimserver.exception
 * @version: 1.0
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException() {
    }

    public TokenExpiredException(String message) {
        super(message);
    }
}
