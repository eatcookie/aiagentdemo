package com.example.aiagentdemo.service;

import com.example.aiagentdemo.service.HybridRetrievalService.RagChunk;
import com.example.aiagentdemo.support.UserContext;
import com.example.aiagentdemo.tools.CourseTools;
import com.example.aiagentdemo.tools.HistoryTools;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * RAG 问答服务：封装「混合检索 → 拼接上下文 → 带工具生成答案 → 记录提问历史」。
 * 由 AiTestController(/api/ai/ask) 与后台 AdminRagController 共同复用，避免逻辑重复。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagQaService {

    /** 引用来源摘要的最大字符数 */
    private static final int SNIPPET_MAX = 200;
    /** 问答结果缓存 key 前缀（按用户隔离，TTL 1 天） */
    private static final String CACHE_PREFIX = "ai:qa:";
    private static final long CACHE_TTL_DAYS = 1;

    private static final String SYSTEM_PROMPT =
            "你是一位 Java 课程助教。请根据问题类型选择回答方式："
                    + "1) 询问课程列表、某个分类或名称下有哪些课程时，必须调用 query_courses 工具，并依据工具返回结果作答；"
                    + "2) 询问当前用户或某用户的提问历史时，必须调用 query_question_history 工具，并依据工具返回结果作答；"
                    + "3) 其他课程知识问题，严格依据【课程片段】回答；若片段信息不足，请明确说明未在课程中找到，不要编造或引入课程外的内容。"
                    + "回答应简洁、条理清晰，同一内容不要重复陈述。";

    private final ChatClient chatClient;
    private final HybridRetrievalService hybridRetrievalService;
    private final RagIngestService ragIngestService;
    private final CourseTools courseTools;
    private final HistoryTools historyTools;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /** 问答结果：答案 + 引用来源。 */
    public record AskResult(String answer, List<Map<String, Object>> sources) {}

    /** 完整 RAG 问答（含 Function Call 工具与提问留痕）。 */
    public AskResult ask(String question, String user) {
        if (question == null || question.isBlank()) {
            return new AskResult("请输入有效的问题。", List.of());
        }
        UserContext.set(user);
        try {
            String cacheKey = CACHE_PREFIX + UserContext.get() + ":" + question.trim();
            AskResult cached = readCache(cacheKey);
            AskResult result;
            if (cached != null) {
                log.info("问答缓存命中：{}", cacheKey);
                result = cached;
            } else {
                result = doAsk(question);
                writeCache(cacheKey, result);
            }
            // 无论是否命中缓存都留痕本次提问（缓存的历史类答案已在首次生成时定格）
            historyTools.recordQuestion(question);
            return result;
        } finally {
            UserContext.clear();
        }
    }

    /** 实际执行「混合检索 → 拼接 → 带工具生成答案」，不含缓存与留痕。 */
    private AskResult doAsk(String question) {
        List<RagChunk> chunks = hybridRetrievalService.search(question);

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RagChunk c = chunks.get(i);
            context.append("【片段").append(i + 1).append("｜").append(c.title()).append("】\n")
                    .append(c.text()).append("\n\n");
        }

        String prompt = "【课程片段】\n" + context + "\n【问题】\n" + question;
        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(prompt)
                .tools(courseTools, historyTools)
                .call()
                .content();

        List<Map<String, Object>> sources = new ArrayList<>();
        for (RagChunk c : chunks) {
            Map<String, Object> src = new HashMap<>();
            src.put("filename", c.filename());
            src.put("title", c.title());
            src.put("snippet", summarize(c.text()));
            sources.add(src);
        }
        return new AskResult(answer, sources);
    }

    /** 读缓存：任何异常只告警并返回 null（降级为正常调用大模型）。 */
    private AskResult readCache(String key) {
        try {
            String json = stringRedisTemplate.opsForValue().get(key);
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, AskResult.class);
        } catch (Exception e) {
            log.warn("读取问答缓存失败（降级直连模型）：{}", e.getMessage());
            return null;
        }
    }

    /** 写缓存：异常只告警，不影响本次回答返回。 */
    private void writeCache(String key, AskResult result) {
        if (result == null || result.answer() == null || result.answer().isBlank()) {
            return;
        }
        try {
            String json = objectMapper.writeValueAsString(result);
            stringRedisTemplate.opsForValue().set(key, json, CACHE_TTL_DAYS, TimeUnit.DAYS);
        } catch (Exception e) {
            log.warn("写入问答缓存失败（忽略）：{}", e.getMessage());
        }
    }

    /** 仅做混合检索（不调大模型），用于后台「检索调试」面板查看召回与 RRF 融合效果。 */
    public List<Map<String, Object>> retrievalDebug(String question) {
        if (question == null || question.isBlank()) {
            return List.of();
        }
        List<RagChunk> chunks = hybridRetrievalService.search(question);
        List<Map<String, Object>> result = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            RagChunk c = chunks.get(i);
            Map<String, Object> item = new HashMap<>();
            item.put("rank", i + 1);
            item.put("title", c.title());
            item.put("filename", c.filename());
            item.put("snippet", summarize(c.text()));
            result.add(item);
        }
        return result;
    }

    /** 覆盖式重新入库，返回写入片段数。 */
    public int reingest() {
        return ragIngestService.ingest();
    }

    private String summarize(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= SNIPPET_MAX ? cleaned : cleaned.substring(0, SNIPPET_MAX) + "…";
    }
}