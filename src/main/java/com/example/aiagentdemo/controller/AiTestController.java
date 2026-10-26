package com.example.aiagentdemo.controller;

import com.example.aiagentdemo.service.RagQaService;
import com.example.aiagentdemo.service.RagQaService.AskResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AiTestController {

    @Autowired
    private ChatClient chatClient;

    @Autowired
    private RagQaService ragQaService;

    @GetMapping("/test")
    public String test() {
        return chatClient.prompt("讲个笑话").call().content();
    }

    @PostMapping("/ask")
    public Map<String, Object> ask(@RequestBody Map<String, String> request) {
        AskResult r = ragQaService.ask(request.get("question"), request.get("user"));
        Map<String, Object> result = new HashMap<>();
        result.put("answer", r.answer());
        result.put("sources", r.sources());
        return result;
    }

    @PostMapping("/reingest")
    public Map<String, Object> reingest() {
        int count = ragQaService.reingest();
        return Map.of("success", count > 0, "ingestedChunks", count);
    }
}