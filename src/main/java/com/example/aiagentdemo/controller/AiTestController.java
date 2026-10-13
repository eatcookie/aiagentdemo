package com.example.aiagentdemo.controller;

import com.example.aiagentdemo.service.HybridRetrievalService;
import com.example.aiagentdemo.service.HybridRetrievalService.RagChunk;
import com.example.aiagentdemo.service.RagIngestService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiTestController {

    /** 引用来源摘要的最大字符数 */
    private static final int SNIPPET_MAX = 200;

    /** 约束模型只依据检索到的课程片段作答，降低幻觉 */
    private static final String SYSTEM_PROMPT =
            "你是一位 Java 课程助教。请严格依据【课程片段】回答【问题】。"
            + "若课程片段中没有足够信息，请明确说明未在课程中找到，不要编造或引入课程外的内容。"
            + "回答应简洁、条理清晰，同一内容不要重复陈述，也不要罗列重复的引用。";

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private HybridRetrievalService hybridRetrievalService;

    @Autowired
    private RagIngestService ragIngestService;

    @GetMapping("/test")
    public String test() {
        return chatClient.prompt("讲个笑话").call().content();
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, String> request) {
        String question = request.get("question");

        // 1. 混合检索（向量 + ES 全文 BM25，RRF 融合，已去重截断）
        List<RagChunk> chunks = hybridRetrievalService.search(question);

        // 2. 拼接上下文（片段正文 + 所属标题），供模型据此作答
        StringBuilder context = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RagChunk c = chunks.get(i);
            context.append("【片段").append(i + 1).append("｜").append(c.title()).append("】\n")
                    .append(c.text()).append("\n\n");
        }

        // 3. 生成回答（用字符串拼接，避免代码块里的 {} 被当作模板占位符解析）
        String prompt = "【课程片段】\n" + context + "\n【问题】\n" + question;
        String answer = chatClient.prompt()
                .system(SYSTEM_PROMPT)
                .user(prompt)
                .call()
                .content();

        // 4. 组装引用来源：标题 / 文件名 / 摘要
        List<Map<String, Object>> sources = new ArrayList<>();
        for (RagChunk c : chunks) {
            Map<String, Object> src = new HashMap<>();
            src.put("filename", c.filename());
            src.put("title", c.title());
            src.put("snippet", summarize(c.text()));
            sources.add(src);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("answer", answer);
        result.put("sources", sources);
        return result;
    }

    @PostMapping("/reingest")
    public Map<String, Object> reingest() {
        int count = ragIngestService.ingest();
        return Map.of("success", count > 0, "ingestedChunks", count);
    }

    /** 截取前 SNIPPET_MAX 个字符作为来源摘要，并压缩多余空白。 */
    private String summarize(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.replaceAll("\\s+", " ").trim();
        return cleaned.length() <= SNIPPET_MAX ? cleaned : cleaned.substring(0, SNIPPET_MAX) + "…";
    }
}
