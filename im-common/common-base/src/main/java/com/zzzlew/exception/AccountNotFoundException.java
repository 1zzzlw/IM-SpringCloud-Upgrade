package com.zzzlew.exception;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/6 - 11 - 06 - 23:43
 * @Description: com.zzzlew.zzzimserver.exception
 * @version: 1.0
 */
public class AccountNotFoundException extends BaseException {
    public AccountNotFoundException() {
    }

    public AccountNotFoundException(String msg) {
        super(msg);
    }
}
