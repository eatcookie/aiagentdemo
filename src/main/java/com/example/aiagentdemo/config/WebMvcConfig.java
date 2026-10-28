package com.example.aiagentdemo.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** MVC 配置：注册鉴权拦截器 + 为 /admin 提供静态页入口（转发到 index.html）。 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final AdminAuthInterceptor adminAuthInterceptor;
    private final RateLimitInterceptor rateLimitInterceptor;
    private final RequestLogInterceptor requestLogInterceptor;

    public WebMvcConfig(AdminAuthInterceptor adminAuthInterceptor,
                        RateLimitInterceptor rateLimitInterceptor,
                        RequestLogInterceptor requestLogInterceptor) {
        this.adminAuthInterceptor = adminAuthInterceptor;
        this.rateLimitInterceptor = rateLimitInterceptor;
        this.requestLogInterceptor = requestLogInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // AI 问答是高价/慢接口，优先限流（登录用户按人、匿名按 IP，每分钟 N 次）
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/ai/ask", "/api/admin/rag/ask");
        registry.addInterceptor(adminAuthInterceptor)
                .addPathPatterns("/api/admin/**")
                .excludePathPatterns("/api/admin/login", "/api/admin/register");
        // 日志拦截器最后注册：afterCompletion 逆序执行，可先于鉴权清理前读到 UserContext
        registry.addInterceptor(requestLogInterceptor)
                .addPathPatterns("/api/**");
    }

    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/admin").setViewName("forward:/admin/index.html");
        registry.addViewController("/admin/").setViewName("forward:/admin/index.html");
    }
}