package com.example.aiagentdemo.service; // ⚠️ 改成你自己的包名

//这段代码会在项目启动时自动读取 MD 文件、切块、转向量并存入 ES

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.Conflicts;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RAG 文档入库服务：读取 resources/document/*.md，切块后向量化写入 ES 向量库。
 * 采用「覆盖式入库」——入库前会先按 filename 删除同一文件的旧向量，因此重复调用不会产生重复数据。
 *
 * ======================= 使用步骤（重要，按此操作即可）=======================
 * 1) 平时正常运行项目：什么都不用做。
 *    app.rag.ingest-on-startup=false，启动不会自动入库，ES 里已有的数据会一直被复用。
 * 2) 第一次搭建 / 或改动了 java.md 之后：手动触发一次重新入库即可（不会重复）：
 *    在 PowerShell 执行：  curl -X POST http://localhost:8080/api/ai/reingest
 *    或调用接口 POST /api/ai/reingest，返回 {"success":true,"ingestedChunks":N} 即成功。
 * 3) 想清空整个知识库：先删 ES 索引再重新入库：
 *    DELETE http://localhost:9200/spring-ai-document-index  （之后重启一次应用重建索引，再调用 /reingest）
 * 注意：修改 embedding 模型或向量维度后，必须删索引重建，否则维度不一致会报错。
 * ===========================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class RagIngestService implements ApplicationRunner {

    /** DashScope text-embedding 单次请求最大文本条数 */
    private static final int DASHSCOPE_MAX_BATCH = 25;
    /** 文档元数据中保存来源文件名的 key */
    private static final String META_FILENAME = "filename";
    /** 文档元数据中保存所属标题（章节）的 key，用于回答上下文与引用来源 */
    private static final String META_TITLE = "title";

    private final VectorStore vectorStore;
    private final ElasticsearchClient elasticsearchClient;

    @Value("${spring.ai.vectorstore.elasticsearch.index-name:spring-ai-document-index}")
    private String indexName;

    @Value("${app.rag.ingest-on-startup:false}")
    private boolean ingestOnStartup;

    @Value("${app.rag.batch-size:10}")
    private int batchSize;

    /** 单个文本块的最大字符数：结构切分后若超长，再按此长度二次切分 */
    @Value("${app.rag.chunking.max-chars:800}")
    private int maxChunkChars;

    /** 二次定长切分时相邻块的重叠字符数（约为块长的 10%，用于缝合被切断的语义） */
    @Value("${app.rag.chunking.overlap:80}")
    private int chunkOverlap;

    @Override
    public void run(ApplicationArguments args) {
        if (!ingestOnStartup) {
            log.info("跳过启动自动入库（app.rag.ingest-on-startup=false）。如需入库请调用 POST /api/ai/reingest");
            return;
        }
        ingest();
    }

    /**
     * 执行一次「覆盖式」文档入库：先删同名文件旧向量，再分批写入。
     * 返回写入的文本块数量（失败返回 0，不抛出异常以免阻断主流程）。
     */
    public int ingest() {
        log.info("开始加载 Markdown 文档到 ES 向量库...");
        try {
            List<Document> allDocuments = loadDocuments();
            if (allDocuments.isEmpty()) {
                log.warn("未找到任何 MD 文档，请检查 resources/document 目录");
                return 0;
            }

            // 1. 清空整个索引，保证「覆盖式入库」不会累积重复（本知识库由 document/*.md 全量派生）
            purgeAllDocuments();

            // 2. 分批写入（批大小限制在 [1, 25]，规避 DashScope 单次 embedding 上限）
            int total = allDocuments.size();
            int effectiveBatch = Math.max(1, Math.min(batchSize, DASHSCOPE_MAX_BATCH));
            for (int i = 0; i < total; i += effectiveBatch) {
                int end = Math.min(i + effectiveBatch, total);
                List<Document> batch = new ArrayList<>(allDocuments.subList(i, end));
                vectorStore.add(batch);
                log.info("已写入一批：本批 {} 个文本块，累计 {}/{}", batch.size(), end, total);
            }

            log.info("🎉 文档写入 ES 成功！共写入 {} 个文本块", total);
            return total;
        } catch (Exception e) {
            log.error("文档入库失败，应用仍可继续运行，请检查 DashScope 配置/网络/ES 状态：{}", e.getMessage(), e);
            return 0;
        }
    }

    /**
     * 清空整个向量索引（用于覆盖式入库）。
     * 直接对索引执行 deleteByQuery(match_all)，避免依赖元数据过滤器的相似度检索
     * 因过滤器/得分阈值未命中而导致旧向量删不掉、重复累积的问题。
     */
    private void purgeAllDocuments() {
        try {
            elasticsearchClient.deleteByQuery(d -> d
                    .index(indexName)
                    .query(q -> q.matchAll(m -> m))
                    .conflicts(Conflicts.Proceed)
                    .refresh(true));
            log.info("已清空索引[{}]中的全部旧向量", indexName);
        } catch (Exception e) {
            log.warn("清空索引[{}]失败（首次入库索引不存在可忽略）：{}", indexName, e.getMessage());
        }
    }

    private List<Document> loadDocuments() throws IOException {
        ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:document/*.md");

        List<Document> allDocuments = new ArrayList<>();
        for (Resource resource : resources) {
            String filename = resource.getFilename();
            log.info("正在解析文档: {}", filename);
            String raw;
            try (var in = resource.getInputStream()) {
                raw = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            chunkByHeadings(raw, filename, allDocuments);
        }
        return allDocuments;
    }

    /**
     * 结构优先切分：以 Markdown 标题（#/##/###…）为边界，每个「标题行 + 其正文」构成一个文本块。
     * 标题既写入 metadata.title，也保留在块正文里（提升向量与全文 BM25 对该小节关键词的命中）。
     * 若某小节超长，再按定长 + overlap 二次切分（每块沿用该小节标题）。代码围栏(```)内的 # 不视为标题。
     */
    private void chunkByHeadings(String raw, String filename, List<Document> out) {
        String[] lines = raw.split("\\R", -1);
        StringBuilder body = new StringBuilder();
        String heading = null;
        boolean inCode = false;
        for (String line : lines) {
            String trimmed = line.trim();
            if (trimmed.startsWith("```")) {
                inCode = !inCode;
                body.append(line).append('\n');
                continue;
            }
            boolean isHeading = !inCode && trimmed.startsWith("#");
            if (isHeading) {
                emit(out, heading, body.toString(), filename);
                body.setLength(0);
                heading = trimmed;
            } else {
                body.append(line).append('\n');
            }
        }
        emit(out, heading, body.toString(), filename);
    }

    /** 生成一个文本块：标题行 + 正文；正文为空则跳过（不入库纯标题行）；超长则二次切分。 */
    private void emit(List<Document> out, String heading, String bodyText, String filename) {
        String body = bodyText == null ? "" : bodyText.strip();
        if (body.isEmpty()) {
            return;
        }
        String title = heading == null ? filename : heading.replaceAll("^#+\\s*", "").trim();
        String fullText = (heading == null ? "" : heading + "\n") + body;
        if (fullText.length() <= maxChunkChars) {
            out.add(buildDocument(fullText, title, filename));
        } else {
            for (String window : splitWithOverlap(fullText, maxChunkChars, chunkOverlap)) {
                out.add(buildDocument(window, title, filename));
            }
        }
    }

    /** 构建带 filename/title 元数据的文本块 Document。 */
    private Document buildDocument(String text, String title, String filename) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put(META_FILENAME, filename);
        metadata.put(META_TITLE, title);
        return Document.builder()
                .text(text)
                .metadata(metadata)
                .build();
    }

    /** 按固定长度 + 重叠滑窗切分长文本，保证不遗漏尾部且相邻块共享 overlap 内容。 */
    private List<String> splitWithOverlap(String text, int size, int overlap) {
        List<String> chunks = new ArrayList<>();
        int step = Math.max(1, size - overlap);
        for (int start = 0; start < text.length(); start += step) {
            int end = Math.min(start + size, text.length());
            chunks.add(text.substring(start, end));
            if (end >= text.length()) {
                break;
            }
        }
        return chunks;
    }
}