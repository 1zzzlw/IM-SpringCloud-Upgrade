package com.zzzlew.aop;

import com.zzzlew.annotaion.UrlLimit;
import com.zzzlew.exception.IPException;
import com.zzzlew.utils.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Aspect
@Component
@Slf4j
public class UrlLimitAspect {
    /**
     * 限流计数器
     */
    private final ConcurrentHashMap<String, AtomicInteger> counterMap = new ConcurrentHashMap<>();

    @Pointcut("@annotation(com.zzzlew.annotaion.UrlLimit)")
    public void rateLimitPointcut() {
    }

    @Around("rateLimitPointcut() && @annotation(urlLimit)")
    public Object around(ProceedingJoinPoint joinPoint, UrlLimit urlLimit) throws Throwable {
        // 获取正确的限流KEY
        String key = getKey(joinPoint);
        AtomicInteger counter = counterMap.computeIfAbsent(key, k -> new AtomicInteger(0));

        // 2. 计数+1，判断是否超限
        int currentCount = counter.incrementAndGet();
        if (currentCount > urlLimit.maxRequests()) {
            log.warn("接口限流触发: key={}, 当前请求数={}, 最大限制={}", key, currentCount, urlLimit.maxRequests());
            throw new IPException("请求过于频繁，请1分钟后重试");
        }

        try {
            // 3. 执行业务逻辑
            return joinPoint.proceed();
        } finally {
            // 请求结束后，计数器-1
            counter.decrementAndGet();
            // 如果计数器为0，移除key，节省内存
            if (counter.get() <= 0) {
                counterMap.remove(key);
            }
        }
    }

    /**
     * 真实客户端IP，需要适配nginx集群
     */
    private String getKey(ProceedingJoinPoint joinPoint) {
        HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.getRequestAttributes()).getRequest();
        String key;

        // 优先拿登录用户ID
        if (UserHolder.getUser() != null) {
            key = "user:" + UserHolder.getUser().getId();
        } else {
            // 从请求头获取真实IP（Nginx配置了X-Real-IP）
            key = "ip:" + getRealIp(request);
        }
        // 实现 每个用户/IP + 每个接口 独立限流
        return key + ":" + request.getRequestURI();
    }

    /**
     * 获取Nginx代理后的真实客户端IP
     */
    private String getRealIp(HttpServletRequest request) {
        // 优先从Nginx传递的请求头获取真实IP
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            return realIp;
        }
        // 备用请求头
        realIp = request.getHeader("X-Forwarded-For");
        if (realIp != null && !realIp.isBlank() && !"unknown".equalsIgnoreCase(realIp)) {
            // 获取第一个IP（多级代理时）
            return realIp.split(",")[0].trim();
        }
        // 兜底
        return request.getRemoteAddr();
    }

    /**
     * 每分钟清空一次计数器（兜底，防止内存泄漏）
     */
    @Scheduled(fixedRate = 60000)
    public void clearCounter() {
        counterMap.clear();
        log.debug("限流计数器已清空");
    }
}