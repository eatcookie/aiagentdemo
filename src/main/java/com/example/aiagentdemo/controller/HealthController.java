package com.example.aiagentdemo.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/health")
    public String health() {
        return "OK";
    }
}

/**
 * @RestController 这个注解的意思是：
 * 告诉Spring，这个类是一个"控制器"，专门用来处理前端/客户端发过来的HTTP请求。
 * 而且它返回的是"数据"（比如这里的"OK"字符串），不是网页页面。
 * 相当于同时加了 @Controller + @ResponseBody 两个注解。
 */
/**
 * @GetMapping("/health") 这个注解的意思是：
 * 把浏览器/Apifox访问的地址 "http://localhost:8080/health"
 * 和下面这个 health() 方法绑定在一起。
 * 也就是说：当有人用 GET 方式访问 /health 这个路径时，
 * Spring 就会自动调用这个方法，并把方法的返回值发回给调用者。
 */