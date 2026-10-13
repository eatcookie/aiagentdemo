package com.example.aiagentdemo.controller;

import com.example.aiagentdemo.service.UserService;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api")
public class AuthController {
    private final UserService userService;
    private final StringRedisTemplate stringRedisTemplate;

    public AuthController(UserService userService, StringRedisTemplate stringRedisTemplate) {
        this.userService = userService;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @PostMapping("/login")
    public String login(@RequestParam String username, @RequestParam String password) {
        // 1. 校验账号密码
        if (userService.login(username, password).startsWith("登录成功")) {
            // 2. 生成简单 Token
            String token = UUID.randomUUID().toString();
            // 3. 存入 Redis，key 为 token，value 为用户名，过期时间 1 小时
            stringRedisTemplate.opsForValue().set(token, username, 1, TimeUnit.HOURS);
            // 4. 返回 Token 给前端
            return "登录成功！你的Token是：" + token;
        }
        return "用户名或密码错误";
    }
}