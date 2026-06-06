package com.zzzlew.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/3 - 06 - 03 - 16:00
 * @Description: com.zzzlew.properties
 * @version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {
    // 短期token拦截排除的路径
    private List<String> excludePaths;
    // 刷新token拦截的路径
    private List<String> refreshPaths;
}
