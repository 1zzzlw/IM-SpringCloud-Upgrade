package com.zzzlew.exception;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/14 - 11 - 14 - 23:49
 * @Description: com.zzzlew.zzzimserver.exception
 * @version: 1.0
 */
public class TokenNotFoundException extends RuntimeException {
    public TokenNotFoundException() {
    }

    public TokenNotFoundException(String message) {
        super(message);
    }
}
