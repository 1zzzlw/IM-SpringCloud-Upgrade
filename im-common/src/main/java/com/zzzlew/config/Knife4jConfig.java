package com.zzzlew.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @Auther: zzzlew
 * @Date: 2025/12/8 - 12 - 08 - 0:43
 * @Description: com.zzzlew.zzzimserver.config
 * @version: 1.0
 */
@Configuration
@ConditionalOnClass(GroupedOpenApi.class)
public class Knife4jConfig {

    // 全局 OpenAPI 基本信息
    @Bean
    public OpenAPI imOpenApi() {
        return new OpenAPI().info(new Info().title("IM 即时通信项目").description("IM 项目接口文档").version("v1.0.0")
                .contact(new Contact().name("zzzzlew").email("1400377637@qq.com")));
    }

    // 指定扫描 controller 包
    @Bean
    public GroupedOpenApi defaultGroup() {
        return GroupedOpenApi.builder().group("default").packagesToScan("com.zzzlew.controller").build();
    }

}
