package com.example.aiagentdemo.support;

/** 请求级用户上下文：ask 入口写入，供工具方法读取当前用户（轻量实现）。 */
public final class UserContext {

    public static final String DEFAULT_USER = "default";

    private static final ThreadLocal<String> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(String user) {
        HOLDER.set(user == null || user.isBlank() ? DEFAULT_USER : user.trim());
    }

    public static String get() {
        String user = HOLDER.get();
        return user == null ? DEFAULT_USER : user;
    }

    public static void clear() {
        HOLDER.remove();
    }
}