package com.zzzlew.result;

import lombok.Data;

/**
 * @Auther: zzzlew
 * @Date: 2025/12/30 - 12 - 30 - 19:49
 * @Description: com.zzzlew.zzzimserver.result
 * @version: 1.0
 */
@Data
public class TokenResult {
    private String accessToken;

    private String refreshToken;
}
