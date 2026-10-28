package com.example.aiagentdemo.config;

import com.example.aiagentdemo.common.Result;
import com.example.aiagentdemo.support.TokenStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.concurrent.TimeUnit;

/** Redis 固定窗口限流：登录用户按用户名、未登录按客户端 IP，每人每分钟最多 N 次，超限返回 429。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitInterceptor implements HandlerInterceptor {

    private static final String PREFIX = "ai:limit:";

    private final StringRedisTemplate stringRedisTemplate;
    private final TokenStore tokenStore;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.rate-limit.per-minute:10}")
    private int perMinute;

    @Value("${app.rate-limit.enabled:true}")
    private boolean enabled;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enabled || "OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String identity = resolveIdentity(request);
        long window = System.currentTimeMillis() / 60_000;
        String key = PREFIX + identity + ":" + window;
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                stringRedisTemplate.expire(key, 60, TimeUnit.SECONDS);
            }
            if (count != null && count > perMinute) {
                log.warn("限流拦截：identity={} 本分钟第 {} 次，上限 {}", identity, count, perMinute);
                response.setStatus(429);
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write(objectMapper.writeValueAsString(
                        Result.error(429, "请求过于频繁，每人每分钟最多 " + perMinute + " 次，请稍后再试")));
                return false;
            }
        } catch (Exception e) {
            // 与检索服务一致的降级策略：Redis 故障只告警放行，不影响问答主流程
            log.warn("限流组件异常（放行）：{}", e.getMessage());
        }
        return true;
    }

    /** 有有效 Token 按用户名计，否则按客户端 IP 兜底计数。 */
    private String resolveIdentity(HttpServletRequest request) {
        String token = request.getHeader("X-Token");
        if (token == null || token.isBlank()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }
        String user = tokenStore.resolve(token);
        if (user != null) {
            return "u:" + user;
        }
        String xff = request.getHeader("X-Forwarded-For");
        String ip = (xff == null || xff.isBlank()) ? request.getRemoteAddr() : xff.split(",")[0].trim();
        return "ip:" + ip;
    }
}