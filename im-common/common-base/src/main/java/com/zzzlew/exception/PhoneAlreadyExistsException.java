package com.zzzlew.exception;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/11 - 11 - 11 - 19:56
 * @Description: com.zzzlew.zzzimserver.exception
 * @version: 1.0
 */
public class PhoneAlreadyExistsException extends RuntimeException {
    public PhoneAlreadyExistsException() {
    }

    public PhoneAlreadyExistsException(String message) {
        super(message);
    }
}
