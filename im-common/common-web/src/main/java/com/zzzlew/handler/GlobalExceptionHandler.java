package com.zzzlew.handler;

import com.zzzlew.exception.BaseException;
import com.zzzlew.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * @Auther: zzzlew
 * @Date: 2026/3/12 - 03 - 12 - 21:24
 * @Description: com.zzzlew.handler
 * @version: 1.0
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 全局异常处理
     *
     * @param ex
     * @return
     */
    @ExceptionHandler
    public Result<String> exceptionHandler(BaseException ex) {
        log.error("[系统异常] {}", ex.getMessage());
        return Result.error(ex.getMessage());
    }


}
