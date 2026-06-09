package com.zzzlew.utils;

import io.jsonwebtoken.*;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

/**
 * @Auther: zzzlew
 * @Date: 2025/11/7 - 11 - 07 - 0:06
 * @Description: com.zzzlew.zzzimserver.utils
 * @version: 1.0
 */
public class JwtUtil {

    // 生成token
    public static String createJWT(String secretKey, long expiration, Map<String, Object> claims) {
        // 指定签名的时候使用的签名算法，也就是header那部分
        SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;

        long expirationTime = System.currentTimeMillis() + expiration;
        Date exp = new Date(expirationTime);

        JwtBuilder builder = Jwts.builder().setClaims(claims).setExpiration(exp).signWith(signatureAlgorithm,
                secretKey.getBytes(StandardCharsets.UTF_8));

        return builder.compact();
    }

    // 解析token
    public static Claims parseJWT(String secretKey, String token) {
        try {
            Claims claims =
                    Jwts.parser().setSigningKey(secretKey.getBytes(StandardCharsets.UTF_8)).parseClaimsJws(token).getBody();
            return claims;
        } catch (ExpiredJwtException e) {
            return null;
        } catch (Exception e) {
            // 处理其他JWT异常
            return null;
        }
    }

}
