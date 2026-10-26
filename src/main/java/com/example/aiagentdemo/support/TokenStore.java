package com.example.aiagentdemo.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 管理后台 Token 存储：签发/校验均走 Redis（key = ai:token:{token}，value = username，TTL 2 小时）。 */
@Component
public class TokenStore {

    private static final String PREFIX = "ai:token:";
    private static final long TTL_HOURS = 2;

    private final StringRedisTemplate redis;

    public TokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public String issue(String username) {
        String token = UUID.randomUUID().toString().replace("-", "");
        redis.opsForValue().set(PREFIX + token, username, TTL_HOURS, TimeUnit.HOURS);
        return token;
    }

    /** 校验并返回用户名；无效/过期返回 null。 */
    public String resolve(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        return redis.opsForValue().get(PREFIX + token.trim());
    }

    public void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            redis.delete(PREFIX + token.trim());
        }
    }
}