package com.zzzlew;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.zzzlew.mapper")
public class ZzzImServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(ZzzImServerApplication.class, args);
    }
}
