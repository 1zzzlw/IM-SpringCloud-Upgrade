package com.zzzlew;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/1 - 06 - 01 - 20:48
 * @Description: com.zzzlew
 * @version: 1.0
 */
@EnableFeignClients(basePackages = "com.zzzlew.client")
@SpringBootApplication
@MapperScan("com.zzzlew.mapper")
public class AuthApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthApplication.class, args);
    }
}
