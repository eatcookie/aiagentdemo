package com.example.aiagentdemo.service;

import com.example.aiagentdemo.common.BizException;
import com.example.aiagentdemo.entity.User;
import com.example.aiagentdemo.mapper.UserMapper;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class UserService {
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserMapper userMapper, PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.passwordEncoder = passwordEncoder;
    }

    public String login(String username, String password) {
        User user = userMapper.findByUsername(username);
        if (user != null && passwordEncoder.matches(password, user.getPassword())) {
            return "登录成功，token生成中（Day3先返回固定字符串）";
        }
        return "用户名或密码错误";
    }

    /** 管理后台登录校验：成功返回用户名，失败返回 null。 */
    public String authenticate(String username, String password) {
        if (username == null || username.isBlank() || password == null) {
            return null;
        }
        User user = userMapper.findByUsername(username.trim());
        return (user != null && passwordEncoder.matches(password, user.getPassword())) ? user.getUsername() : null;
    }

    /** 用户注册：校验唯一性后用 BCrypt 加密密码写入 t_user。 */
    public void register(String username, String password) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new BizException("用户名和密码不能为空");
        }
        String u = username.trim();
        if (userMapper.countByUsername(u) > 0) {
            throw new BizException("用户名已存在");
        }
        User user = new User();
        user.setUsername(u);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole("user");
        userMapper.insert(user);
    }

    /** 用户列表（脱敏，不含密码）。 */
    public List<Map<String, Object>> listUsers() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (User u : userMapper.listAll()) {
            Map<String, Object> item = new HashMap<>();
            item.put("id", u.getId());
            item.put("username", u.getUsername());
            item.put("role", u.getRole());
            result.add(item);
        }
        return result;
    }
}
