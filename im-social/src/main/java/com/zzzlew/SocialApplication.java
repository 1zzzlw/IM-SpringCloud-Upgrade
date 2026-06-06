package com.zzzlew;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/2 - 06 - 02 - 19:43
 * @Description: com.zzzlew
 * @version: 1.0
 */
@EnableFeignClients("com.zzzlew.client")
@SpringBootApplication
@MapperScan("com.zzzlew.mapper")
public class SocialApplication {
    public static void main(String[] args) {
        SpringApplication.run(SocialApplication.class, args);
    }
}
