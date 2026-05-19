package com.example.smartagent.service.ratelimit;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private static final String RATE_PREFIX = "rate:";

    private final StringRedisTemplate redisTemplate;

    @Value("${rate-limit.default.qps:10}")
    private int defaultQps;

    @Value("${rate-limit.anonymous.qps:2}")
    private int anonymousQps;

    @Value("${rate-limit.default.tokens-per-minute:5000}")
    private int defaultTokensPerMinute;

    public RateLimitResult allowRequest(String userId) {
        return allowRequest(userId, "default");
    }

    public RateLimitResult allowRequest(String userId, String tenantId) {
        int limit = getQpsLimit(userId, tenantId);
        String key = RATE_PREFIX + tenantId + ":" + userId;

        try {
            Long current = redisTemplate.opsForValue().increment(key);

            if (current == null) {
                current = 1L;
            }

            if (current == 1) {
                redisTemplate.expire(key, 1, TimeUnit.SECONDS);
            }

            if (current > limit) {
                log.warn("Rate limit exceeded for user: {}, current: {}, limit: {}", userId, current, limit);
                return RateLimitResult.rejected(current.intValue(), limit, 1);
            }

            return RateLimitResult.allowed(current.intValue(), limit);

        } catch (Exception e) {
            log.error("Rate limit check failed", e);
            return RateLimitResult.allowed(0, limit);
        }
    }

    public RateLimitResult allowTokenRequest(String userId, int tokens) {
        return allowTokenRequest(userId, "default", tokens);
    }

    public RateLimitResult allowTokenRequest(String userId, String tenantId, int tokens) {
        int limit = defaultTokensPerMinute;
        String key = "token-rate:" + tenantId + ":" + userId;

        try {
            Long current = redisTemplate.opsForValue().increment(key, tokens);

            if (current == null) {
                current = (long) tokens;
            }

            if (current.equals((long) tokens)) {
                redisTemplate.expire(key, 60, TimeUnit.SECONDS);
            }

            if (current > limit) {
                redisTemplate.opsForValue().decrement(key, tokens);
                log.warn("Token rate limit exceeded for user: {}, current: {}, limit: {}", userId, current, limit);
                return RateLimitResult.rejected(current.intValue(), limit, 60);
            }

            return RateLimitResult.allowed(current.intValue(), limit);

        } catch (Exception e) {
            log.error("Token rate limit check failed", e);
            return RateLimitResult.allowed(0, limit);
        }
    }

    private int getQpsLimit(String userId, String tenantId) {
        if (userId == null || "anonymous".equals(userId)) {
            return anonymousQps;
        }
        return defaultQps;
    }

    public void resetLimit(String userId) {
        resetLimit(userId, "default");
    }

    public void resetLimit(String userId, String tenantId) {
        String key = RATE_PREFIX + tenantId + ":" + userId;
        redisTemplate.delete(key);
        log.info("Reset rate limit for user: {}, tenant: {}", userId, tenantId);
    }
}
