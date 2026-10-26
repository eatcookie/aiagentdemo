package com.example.aiagentdemo.controller;

import com.example.aiagentdemo.common.Result;
import com.example.aiagentdemo.service.RagQaService;
import com.example.aiagentdemo.service.RagQaService.AskResult;
import com.example.aiagentdemo.support.UserContext;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 后台 AI 答疑 / 检索调试 / 知识库入库（受 AdminAuthInterceptor 保护）。 */
@RestController
@RequestMapping("/api/admin/rag")
public class AdminRagController {

    private final RagQaService ragQaService;

    public AdminRagController(RagQaService ragQaService) {
        this.ragQaService = ragQaService;
    }

    @PostMapping("/ask")
    public Result<Map<String, Object>> ask(@RequestBody Map<String, String> body) {
        AskResult r = ragQaService.ask(body.get("question"), UserContext.get());
        Map<String, Object> data = new HashMap<>();
        data.put("answer", r.answer());
        data.put("sources", r.sources());
        return Result.ok(data);
    }

    @GetMapping("/retrieval")
    public Result<List<Map<String, Object>>> retrieval(@RequestParam String question) {
        return Result.ok(ragQaService.retrievalDebug(question));
    }

    @PostMapping("/reingest")
    public Result<Map<String, Object>> reingest() {
        int n = ragQaService.reingest();
        Map<String, Object> data = new HashMap<>();
        data.put("success", n > 0);
        data.put("ingestedChunks", n);
        return Result.ok(data);
    }
}