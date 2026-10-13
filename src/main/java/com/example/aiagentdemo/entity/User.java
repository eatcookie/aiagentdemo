package com.example.aiagentdemo.entity;

public class User {
    private Long id;
    private String username;
    private String password;
    private String role;

    // 省略 getter 和 setter（右键 -> Generate 生成）
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}