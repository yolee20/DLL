package com.example.smartagent.service.ratelimit;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitResult {
    private boolean allowed;
    private int current;
    private int limit;
    private int retryAfterSeconds;

    public static RateLimitResult allowed(int current, int limit) {
        return RateLimitResult.builder()
                .allowed(true)
                .current(current)
                .limit(limit)
                .retryAfterSeconds(0)
                .build();
    }

    public static RateLimitResult rejected(int current, int limit, int retryAfterSeconds) {
        return RateLimitResult.builder()
                .allowed(false)
                .current(current)
                .limit(limit)
                .retryAfterSeconds(retryAfterSeconds)
                .build();
    }
}
