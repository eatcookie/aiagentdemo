package com.example.aiagentdemo.support;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/** 管理后台 Token 存储：签发/校验均走 Redis（key = ai:token:{token}，value = username|role，TTL 2 小时）。 */
@Component
public class TokenStore {

    private static final String PREFIX = "ai:token:";
    private static final long TTL_HOURS = 2;
    private static final String SEP = "|";

    private final StringRedisTemplate redis;

    public TokenStore(StringRedisTemplate redis) {
        this.redis = redis;
    }

    /** 认证主体：用户名 + 角色。 */
    public record Principal(String username, String role) {}

    public String issue(String username, String role) {
        String token = UUID.randomUUID().toString().replace("-", "");
        String value = username + SEP + (role == null || role.isBlank() ? "USER" : role.trim());
        redis.opsForValue().set(PREFIX + token, value, TTL_HOURS, TimeUnit.HOURS);
        return token;
    }

    /** 解析 Token 得到用户名与角色；无效/过期返回 null。 */
    public Principal resolvePrincipal(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        String value = redis.opsForValue().get(PREFIX + token.trim());
        if (value == null) {
            return null;
        }
        int idx = value.indexOf(SEP);
        if (idx < 0) {
            return new Principal(value, "USER");
        }
        return new Principal(value.substring(0, idx), value.substring(idx + SEP.length()));
    }

    /** 兼容旧调用：仅返回用户名。 */
    public String resolve(String token) {
        Principal p = resolvePrincipal(token);
        return p == null ? null : p.username();
    }

    public void invalidate(String token) {
        if (token != null && !token.isBlank()) {
            redis.delete(PREFIX + token.trim());
        }
    }
}