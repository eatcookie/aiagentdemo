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
                        .requestMatchers("/api/login", "/health", "/api/ai/**").permitAll()  // 放行登录和健康检查
                        .anyRequest().authenticated()  // 其他接口需要登录
                );
        return http.build();
    }
}
