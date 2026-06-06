package com.zzzlew;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/5 - 06 - 05 - 12:02
 * @Description: com.zzzlew
 * @version: 1.0
 */
@SpringBootApplication
@MapperScan("com.zzzlew.mapper")
@EnableFeignClients(basePackages = "com.zzzlew.client")
public class MomentsApplication {
    public static void main(String[] args) {
        SpringApplication.run(MomentsApplication.class, args);
    }
}
