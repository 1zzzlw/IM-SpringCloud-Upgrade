package com.zzzlew.config;

import com.zzzlew.properties.MinIOConfigProperties;
import io.minio.MinioClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @Auther: zzzlew
 * @Date: 2025/12/13 - 12 - 13 - 18:07
 * @Description: com.zzzlew.zzzimserver.config
 * @version: 1.0
 */
@Slf4j
@Component
@Configuration
public class MinIOConfig {

    @Resource
    private MinIOConfigProperties minIOConfigProperties;

    @Bean
    public MinioClient buildMinioClient() {
        return MinioClient.builder().endpoint(minIOConfigProperties.getEndpoint())
                .credentials(minIOConfigProperties.getAccessKey(), minIOConfigProperties.getSecretKey()).build();
    }

}
