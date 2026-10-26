package com.example.aiagentdemo.controller;

import com.example.aiagentdemo.common.Result;
import com.example.aiagentdemo.service.UserService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/** 后台用户管理（脱敏列表）。 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final UserService userService;

    public AdminUserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public Result<List<Map<String, Object>>> list() {
        return Result.ok(userService.listUsers());
    }
}
