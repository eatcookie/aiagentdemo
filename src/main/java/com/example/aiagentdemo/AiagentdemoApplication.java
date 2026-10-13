package com.example.aiagentdemo;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@MapperScan("com.example.aiagentdemo.mapper")
public class AiagentdemoApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiagentdemoApplication.class, args);
    }

}
