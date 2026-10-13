package com.example.aiagentdemo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestFactory;

import java.time.Duration;

/**
 * 全局 RestClient 超时配置。
 *
 * <p>用途：解决调用 DashScope（通义千问）时出现的
 * {@code SocketTimeoutException: timeout / I/O error on POST ...}。
 * 底层 HTTP 客户端（OkHttp）默认读取超时只有 10~30 秒，而大模型对较长/RAG 回答
 * 的生成常需 60~180 秒，客户端会提前断开。这里将读取超时拉长到分钟级。
 *
 * <p>通过 Spring 官方的 {@link RestClientCustomizer} 扩展点生效，
 * Spring AI Alibaba 自动配置所使用的 {@code RestClient.Builder} 会应用本定制器。
 */
@Configuration
public class RestClientConfig {

    /**
     * 依据配置项构建带超时参数的请求工厂，并应用到所有由 Spring 托管的 RestClient.Builder。
     * 若 okhttp 在 classpath 上，ClientHttpRequestFactories 会自动选择 OkHttp 实现并写入超时。
     */
    @Bean
    public RestClientCustomizer dashScopeTimeoutCustomizer(
            @Value("${app.http.connect-timeout:10s}") Duration connectTimeout,
            @Value("${app.http.read-timeout:300s}") Duration readTimeout) {
        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(connectTimeout)
                .withReadTimeout(readTimeout);
        ClientHttpRequestFactory requestFactory = ClientHttpRequestFactories.get(settings);
        return builder -> builder.requestFactory(requestFactory);
    }
}
