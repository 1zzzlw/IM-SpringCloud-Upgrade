package com.zzzlew.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

/**
 * Redis Lua 脚本配置
 *
 * @author zzzlew
 * @date 2026-06-08
 */
@Configuration
public class RedisLuaScriptConfig {

    /**
     * 点赞/取消点赞 Lua 脚本
     */
    @Bean
    public DefaultRedisScript<Long> momentsLikeScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/moments_like.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 缓存预热 Lua 脚本
     */
    @Bean
    public DefaultRedisScript<Long> momentsWarmupCacheScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/moments_warmup_cache.lua")));
        script.setResultType(Long.class);
        return script;
    }

    /**
     * 删除帖子清理缓存 Lua 脚本
     */
    @Bean
    public DefaultRedisScript<Long> momentsDeleteScript() {
        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource("lua/moments_delete.lua")));
        script.setResultType(Long.class);
        return script;
    }
}
