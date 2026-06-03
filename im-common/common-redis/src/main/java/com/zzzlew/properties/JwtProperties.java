package com.zzzlew.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/7 - 11 - 07 - 0:10
 * @Description: com.zzzlew.properties
 * @version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {
    /**
     * JWT 短Token密钥
     */
    private String accessSecretKey;

    /**
     * JWT 刷新Token密钥
     */
    private String freshSecretKey;

    /**
     * 短期token过期时间，默认1小时
     */
    private Long accessExpiration;

    /**
     * 长期token过期时间，默认1年
     */
    private Long refreshExpiration;

    /**
     * token名称，默认Authorization
     */
    private String tokenName;
}
