package com.zzzlew.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;

/**
 * @Auther: zzzlew
 * @Date: 2026/6/3
 * @Description: Reactive Redis 配置
 * @version: 1.0
 */
@Configuration
public class ReactiveRedisConfig {

    @Bean
    @SuppressWarnings("unchecked")
    public ReactiveRedisTemplate<Object, Object> reactiveRedisTemplate(
            ReactiveRedisConnectionFactory connectionFactory) {

        // 使用 String 序列化器
        RedisSerializer<String> stringSerializer = RedisSerializer.string();

        // 配置序列化上下文
        RedisSerializationContext<Object, Object> serializationContext =
                RedisSerializationContext.<Object, Object>newSerializationContext((RedisSerializer<Object>) (RedisSerializer<?>) stringSerializer)
                        .key((RedisSerializer<Object>) (RedisSerializer<?>) stringSerializer)
                        .value((RedisSerializer<Object>) (RedisSerializer<?>) stringSerializer)
                        .hashKey((RedisSerializer<Object>) (RedisSerializer<?>) stringSerializer)
                        .hashValue((RedisSerializer<Object>) (RedisSerializer<?>) stringSerializer)
                        .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }
}
