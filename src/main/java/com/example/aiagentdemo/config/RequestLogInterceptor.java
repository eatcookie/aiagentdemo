package com.example.aiagentdemo.config;

import com.example.aiagentdemo.support.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 请求访问日志：方法、路径、当前用户、状态码、耗时；异常请求额外打 error。 */
@Slf4j
@Component
public class RequestLogInterceptor implements HandlerInterceptor {

    private static final String ATTR_START = "requestStartTimeMillis";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(ATTR_START, System.currentTimeMillis());
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        Object start = request.getAttribute(ATTR_START);
        long cost = start == null ? -1 : System.currentTimeMillis() - (long) start;
        String line = String.format("%s %s user=%s status=%d cost=%dms",
                request.getMethod(), request.getRequestURI(), UserContext.get(), response.getStatus(), cost);
        if (ex != null) {
            log.error("请求异常 {}", line, ex);
        } else {
            log.info(line);
        }
    }
}