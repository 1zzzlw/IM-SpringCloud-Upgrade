package com.zzzlew;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/2 - 06 - 02 - 19:53
 * @Description: com.zzzlew
 * @version: 1.0
 */
@SpringBootApplication
@MapperScan("com.zzzlew.mapper")
public class ContentApplication {
    public static void main(String[] args) {
        SpringApplication.run(ContentApplication.class, args);
    }
}
