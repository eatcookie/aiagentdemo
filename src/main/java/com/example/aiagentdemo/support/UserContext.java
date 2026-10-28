package com.example.aiagentdemo.support;

/** 请求级用户上下文：ask/鉴权入口写入，供工具与权限校验读取当前用户及其角色（轻量实现）。 */
public final class UserContext {

    public static final String DEFAULT_USER = "default";
    public static final String DEFAULT_ROLE = "USER";

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();
    private static final ThreadLocal<String> ROLE_HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(String user) {
        HOLDER.set(user == null || user.isBlank() ? DEFAULT_USER : user.trim());
    }

    public static String get() {
        String user = HOLDER.get();
        return user == null ? DEFAULT_USER : user;
    }

    public static void setRole(String role) {
        ROLE_HOLDER.set(role == null || role.isBlank() ? DEFAULT_ROLE : role.trim());
    }

    public static String getRole() {
        String role = ROLE_HOLDER.get();
        return role == null ? DEFAULT_ROLE : role;
    }

    public static boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(getRole());
    }

    public static void clear() {
        HOLDER.remove();
        ROLE_HOLDER.remove();
    }
}