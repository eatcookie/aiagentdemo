package com.example.aiagentdemo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityFilterConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())  // 关闭 CSRF（开发阶段方便测试）
                .authorizeHttpRequests(auth -> auth
                        // 演示/健康/AI 接口保持放行
                        .requestMatchers("/api/login", "/health", "/api/ai/**").permitAll()
                        // 管理后台：Security 层整体放行（含登录与数据接口），Token 校验交由 AdminAuthInterceptor
                        .requestMatchers("/admin/**", "/api/admin/**", "/", "/index.html", "/favicon.ico").permitAll()
                        .anyRequest().authenticated()  // 其他接口需要登录
                );
        return http.build();
    }
}
