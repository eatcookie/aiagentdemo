package com.example.aiagentdemo.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.Hit;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 混合检索服务：向量语义召回 + ES 全文(BM25)召回，再用 RRF(排名倒数融合)合并。
 * 相比纯向量检索，可兼顾「标题/关键词精确匹配」与「语义相似」，降低漏检。
 *
 * <p>RRF 公式：score(d) = Σ 1 / (rrfK + rank_i(d))，其中 rank_i 为文档 d 在第 i 路结果中的名次。
 * 只依赖名次、不依赖各路的分数量纲，天然适配「向量分数与 BM25 分数量级不一致」的场景。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class HybridRetrievalService {

    private static final String META_FILENAME = "filename";
    private static final String META_TITLE = "title";

    private final VectorStore vectorStore;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:spring-ai-document-index}")
    private String indexName;

    @Value("${app.rag.retrieval.top-k:10}")
    private int vectorTopK;

    @Value("${app.rag.retrieval.text-top-k:10}")
    private int textTopK;

    @Value("${app.rag.retrieval.similarity-threshold:0.5}")
    private double similarityThreshold;

    @Value("${app.rag.retrieval.max-sources:5}")
    private int maxSources;

    @Value("${app.rag.retrieval.rrf-k:60}")
    private int rrfK;

    /**
     * 全文(BM25)召回的最小匹配门槛：bigram 分词下，泛词（如“什么/是什”）会命中大量无关小节，
     * 要求查询命中的 bigram 占比达到阈值才保留，显著提升全文召回精度（向量召回仍保证覆盖面）。
     */
    @Value("${app.rag.retrieval.text-min-should-match:50%}")
    private String textMinShouldMatch;

    /** 检索到的课程片段。 */
    public record RagChunk(String id, String text, String title, String filename) {}

    /** ES _source 映射：content 正文 + metadata 元数据对象（忽略 embedding/id 等其余字段）。 */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record EsSource(String content, Map<String, Object> metadata) {}

    /**
     * 混合检索入口：返回融合并截断后的若干最相关片段（按 RRF 分数从高到低）。
     */
    public List<RagChunk> search(String question) {
        List<RagChunk> vector = vectorRecall(question);
        List<RagChunk> text = textRecall(question);

        Map<String, Double> scores = new LinkedHashMap<>();
        Map<String, RagChunk> pool = new LinkedHashMap<>();
        accumulate(vector, scores, pool);
        accumulate(text, scores, pool);

        List<RagChunk> merged = new ArrayList<>(scores.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
                .limit(Math.max(0, maxSources))
                .map(e -> pool.get(e.getKey()))
                .toList());
        return merged;
    }

    /** 将一路（已按相关度排序）结果按 RRF 公式累加进总分，并登记片段内容。 */
    private void accumulate(List<RagChunk> ranked, Map<String, Double> scores, Map<String, RagChunk> pool) {
        for (int i = 0; i < ranked.size(); i++) {
            RagChunk chunk = ranked.get(i);
            int rank = i + 1;
            scores.merge(chunk.id(), 1.0 / (rrfK + rank), Double::sum);
            pool.putIfAbsent(chunk.id(), chunk);
        }
    }

    /** 向量语义召回：复用 VectorStore（内部自动对 query 做 embedding）。 */
    private List<RagChunk> vectorRecall(String question) {
        try {
            List<Document> docs = vectorStore.similaritySearch(SearchRequest.builder()
                    .query(question)
                    .topK(vectorTopK)
                    .similarityThreshold(similarityThreshold)
                    .build());
            List<RagChunk> result = new ArrayList<>(docs.size());
            for (Document d : docs) {
                result.add(toChunk(d.getId(), d.getText(), d.getMetadata()));
            }
            return result;
        } catch (Exception e) {
            log.warn("向量召回失败：{}", e.getMessage());
            return List.of();
        }
    }

    /** ES 全文(BM25)召回：multi_match 打在正文与标题上（标题加权）。 */
    private List<RagChunk> textRecall(String question) {
        try {
            SearchResponse<EsSource> resp = elasticsearchClient.search(s -> s
                    .index(indexName)
                    .size(textTopK)
                    // 只取正文与元数据，避免 _source 里的 embedding(1024维)触发未知属性反序列化失败
                    .source(sc -> sc.filter(f -> f.includes(List.of("content", "metadata"))))
                    .query(q -> q.multiMatch(m -> m
                            .query(question)
                            .minimumShouldMatch(textMinShouldMatch)
                            .fields(List.of("content", "metadata.title^2")))),
                    EsSource.class);
            List<RagChunk> result = new ArrayList<>();
            for (Hit<EsSource> hit : resp.hits().hits()) {
                EsSource src = hit.source();
                if (src == null) {
                    continue;
                }
                result.add(toChunk(hit.id(), src.content(), src.metadata()));
            }
            return result;
        } catch (Exception e) {
            log.warn("全文召回失败，已退回仅向量结果：{}", e.getMessage());
            return List.of();
        }
    }

    private RagChunk toChunk(String id, String text, Map<String, Object> metadata) {
        Map<String, Object> md = metadata == null ? Map.of() : metadata;
        String title = String.valueOf(md.getOrDefault(META_TITLE, ""));
        String filename = String.valueOf(md.getOrDefault(META_FILENAME, ""));
        return new RagChunk(id, text, title, filename);
    }
}
