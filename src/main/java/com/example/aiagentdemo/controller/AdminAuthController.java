package com.example.aiagentdemo.controller;

import com.example.aiagentdemo.common.BizException;
import com.example.aiagentdemo.common.Result;
import com.example.aiagentdemo.dto.LoginRequest;
import com.example.aiagentdemo.service.UserService;
import com.example.aiagentdemo.support.TokenStore;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;

/** 管理后台登录：校验账号密码并签发 Token（供前端后续请求带 X-Token）。 */
@RestController
@RequestMapping("/api/admin")
public class AdminAuthController {

    private final UserService userService;
    private final TokenStore tokenStore;

    public AdminAuthController(UserService userService, TokenStore tokenStore) {
        this.userService = userService;
        this.tokenStore = tokenStore;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginRequest request) {
        String user = userService.authenticate(request.getUsername(), request.getPassword());
        if (user == null) {
            throw new BizException(401, "用户名或密码错误");
        }
        String token = tokenStore.issue(user);
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", user);
        return Result.ok(data);
    }

    @PostMapping("/register")
    public Result<Void> register(@RequestBody LoginRequest request) {
        userService.register(request.getUsername(), request.getPassword());
        return Result.ok();
    }

    /** 登出：使当前 Token 在 Redis 中立即失效（失效由 AdminAuthInterceptor 校验，无需放行）。 */
    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String token = request.getHeader("X-Token");
        if (token == null || token.isBlank()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }
        tokenStore.invalidate(token);
        return Result.ok();
    }
}