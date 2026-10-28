package com.example.aiagentdemo.config;

import com.example.aiagentdemo.support.TokenStore;
import com.example.aiagentdemo.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** 管理后台鉴权拦截器：校验 X-Token/Authorization，写入用户与角色，并按 @RequireAdmin 做角色管控。 */
@Component
public class AdminAuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;

    public AdminAuthInterceptor(TokenStore tokenStore) {
        this.tokenStore = tokenStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = request.getHeader("X-Token");
        if (token == null || token.isBlank()) {
            String auth = request.getHeader("Authorization");
            if (auth != null && auth.startsWith("Bearer ")) {
                token = auth.substring(7);
            }
        }
        TokenStore.Principal principal = tokenStore.resolvePrincipal(token);
        if (principal == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
            return false;
        }
        UserContext.set(principal.username());
        UserContext.setRole(principal.role());

        // 声明式管理员校验：方法或类上标注 @RequireAdmin 时，非 ADMIN 直接 403
        if (handler instanceof HandlerMethod hm
                && (hm.hasMethodAnnotation(RequireAdmin.class)
                || hm.getBeanType().isAnnotationPresent(RequireAdmin.class))
                && !"ADMIN".equalsIgnoreCase(principal.role())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":403,\"message\":\"需要管理员权限\",\"data\":null}");
            return false;
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        UserContext.clear();
    }
}