package com.zzzlew.filters;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.zzzlew.constant.RedisConstant;
import com.zzzlew.domain.dto.UserBaseDTO;
import com.zzzlew.properties.AuthProperties;
import com.zzzlew.properties.JwtProperties;
import com.zzzlew.utils.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Map;

import static com.zzzlew.constant.RedisConstant.*;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/3 - 06 - 03 - 15:00
 * @Description: com.zzzlew.filters
 * @version: 1.0
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private final ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate;

    private final JwtProperties jwtproperties;

    private final AuthProperties authProperties;

    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getPath().toString();

        // 先判断请求是否需要拦截
        if (isExclude(path)) {
            return chain.filter(exchange);
        }

        // 获取 Token
        String token = request.getHeaders().getFirst(jwtproperties.getTokenName());
        log.info("网关拦截路径：{}，Token为：{}", path, token);

        // 判断是否是"长期Token"业务路径
        if (isRefreshPath(path)) {
            // 长期 Token (Refresh) 逻辑
            return handleRefreshToken(exchange, chain, response, path, token);
        } else {
            // 短期 Token (Access) 逻辑
            return handleAccessToken(exchange, chain, response, path, token);
        }
    }

    /**
     * 处理长期 Token（Refresh Token）
     */
    private Mono<Void> handleRefreshToken(ServerWebExchange exchange, GatewayFilterChain chain,
                                          ServerHttpResponse response, String path, String token) {
        if (StrUtil.isBlank(token)) {
            log.error("长期token为空，拒绝访问: {}", path);
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return response.setComplete();
        }

        // 解析长期 Token
        if (JwtUtil.parseJWT(jwtproperties.getFreshSecretKey(), token) == null) {
            log.error("刷新token过期，需要重新登录");
            response.setStatusCode(HttpStatus.BAD_REQUEST);
            return response.setComplete();
        }

        // 长期 Token 的 Redis 操作（响应式）
        String refreshTokenKey = LOGIN_USERINFO_REFRESHTOKEN_KEY + token;

        return reactiveRedisTemplate.opsForHash().entries(refreshTokenKey)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(userMap -> {
                    if (userMap.isEmpty()) {
                        log.error("长期token在Redis中不存在: {}", token);
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return response.setComplete();
                    }

                    // 刷新长期 token 在 redis 中的过期时间
                    return reactiveRedisTemplate.expire(refreshTokenKey,
                                    Duration.ofMinutes(LOGIN_USERINFO_REFRESHTOKEN_KEY_TTL))
                            .flatMap(success -> {
                                // 透传用户信息并放行
                                return passThrough(exchange, chain, userMap);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Redis操作异常: {}", e.getMessage());
                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return response.setComplete();
                });
    }

    /**
     * 处理短期 Token（Access Token）
     */
    private Mono<Void> handleAccessToken(ServerWebExchange exchange, GatewayFilterChain chain,
                                         ServerHttpResponse response, String path, String token) {
        if (StrUtil.isBlank(token)) {
            log.error("短期token为空，拒绝访问: {}", path);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 解析短期 Token
        if (JwtUtil.parseJWT(jwtproperties.getAccessSecretKey(), token) == null) {
            log.error("短期token过期或无效");
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 短期 Token 的 Redis 操作（响应式）
        String accessTokenKey = RedisConstant.LOGIN_USERINFO_ACCESSTOKEN_KEY + token;

        return reactiveRedisTemplate.opsForHash().entries(accessTokenKey)
                .collectMap(Map.Entry::getKey, Map.Entry::getValue)
                .flatMap(userMap -> {
                    if (userMap.isEmpty()) {
                        log.error("短期token在Redis中不存在，可能已被清理或失效: {}", token);
                        response.setStatusCode(HttpStatus.UNAUTHORIZED);
                        return response.setComplete();
                    }

                    // 刷新短期 token 在 redis 中的过期时间
                    UserBaseDTO userBaseDTO = BeanUtil.copyProperties(userMap, UserBaseDTO.class);
                    String userTokenSetKey = RedisConstant.LOGIN_USER_TOKEN_LIST_KEY + userBaseDTO.getId();

                    return reactiveRedisTemplate.expire(accessTokenKey,
                                    Duration.ofMinutes(LOGIN_USERINFO_ACCESSTOKEN_KEY_TTL))
                            .flatMap(success1 ->
                                    reactiveRedisTemplate.expire(userTokenSetKey,
                                            Duration.ofMinutes(LOGIN_USER_TOKEN_LIST_KEY_TTL))
                            )
                            .flatMap(success2 -> {
                                // 透传用户信息并放行
                                return passThrough(exchange, chain, userMap);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Redis操作异常: {}", e.getMessage());
                    response.setStatusCode(HttpStatus.INTERNAL_SERVER_ERROR);
                    return response.setComplete();
                });
    }

    /**
     * 封装用户信息到 Header 并向下游传递
     */
    private Mono<Void> passThrough(ServerWebExchange exchange, GatewayFilterChain chain, Map<Object, Object> userMap) {
        String userInfoJson = JSONUtil.toJsonStr(userMap);
        ServerHttpRequest modifiedRequest = exchange.getRequest().mutate()
                .header("user-info", userInfoJson)
                .build();
        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private boolean isExclude(String path) {
        for (String pathPattern : authProperties.getExcludePaths()) {
            if (antPathMatcher.match(pathPattern, path)) {
                return true;
            }
        }
        return false;
    }

    private boolean isRefreshPath(String path) {
        if (authProperties.getRefreshPaths() == null) return false;
        for (String pattern : authProperties.getRefreshPaths()) {
            if (antPathMatcher.match(pattern, path)) return true;
        }
        return false;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
