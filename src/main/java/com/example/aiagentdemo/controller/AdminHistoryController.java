package com.example.aiagentdemo.controller;

import com.example.aiagentdemo.common.Result;
import com.example.aiagentdemo.tools.HistoryTools;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 后台查看提问历史（Redis）；user 省略时默认当前登录用户。 */
@RestController
@RequestMapping("/api/admin")
public class AdminHistoryController {

    private final HistoryTools historyTools;

    public AdminHistoryController(HistoryTools historyTools) {
        this.historyTools = historyTools;
    }

    @GetMapping("/history")
    public Result<Map<String, Object>> history(@RequestParam(required = false) String user) {
        return Result.ok(historyTools.queryQuestionHistory(user));
    }
}