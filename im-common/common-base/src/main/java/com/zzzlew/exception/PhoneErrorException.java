package com.zzzlew.exception;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/10 - 11 - 10 - 23:53
 * @Description: com.zzzlew.zzzimserver.exception
 * @version: 1.0
 */
public class PhoneErrorException extends RuntimeException {

    public PhoneErrorException() {
    }

    public PhoneErrorException(String message) {
        super(message);
    }
}
