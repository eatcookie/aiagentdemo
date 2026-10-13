package com.example.aiagentdemo.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.InputStream;

/**
 * 启动时预建向量索引。
 *
 * <p>因为 {@code spring.ai.vectorstore.elasticsearch.initialize-schema=false}，Spring AI 不再自动建索引；
 * 而默认的 standard 分析器不能对中文做全文检索。这里用内置 {@code ngram} 二元组(bigram)分析器（{@code classpath:es/document-index-mapping.json}）
 * 预建索引，使 ES 全文(BM25)能命中中文，同时保持 embedding 为 dense_vector/dims/cosine 与 Spring AI 兼容。
 *
 * <p>策略：索引不存在才创建，已存在则跳过（避免每次启动清空已入库数据）。改维度/分析器时，
 * 先删除该索引再重启，让本组件按新 mapping 重建，然后调用 /reingest 重新入库。
 *
 * <p>{@code @Order(0)} 保证早于入库与检索相关的 Runner 执行。
 */
@Slf4j
@Component
@Order(0)
@RequiredArgsConstructor
public class ElasticsearchIndexInitializer implements ApplicationRunner {

    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:spring-ai-document-index}")
    private String indexName;

    @Value("${app.rag.ensure-index.enabled:true}")
    private boolean ensureIndexEnabled;

    @Override
    public void run(ApplicationArguments args) {
        if (!ensureIndexEnabled) {
            log.info("已跳过启动预建索引（app.rag.ensure-index.enabled=false）");
            return;
        }
        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            if (exists) {
                log.info("向量索引[{}]已存在，跳过预建（如需按新 mapping 重建，请先删除该索引再重启）", indexName);
                return;
            }
            try (InputStream is = new ClassPathResource("es/document-index-mapping.json").getInputStream()) {
                elasticsearchClient.indices().create(c -> c.index(indexName).withJson(is));
            }
            log.info("🧱 已用中文(ngram bigram)分析器预建向量索引[{}]", indexName);
        } catch (Exception e) {
            log.warn("预建向量索引[{}]失败，请检查 ES 状态/映射文件：{}", indexName, e.getMessage());
        }
    }
}
