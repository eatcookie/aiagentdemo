package com.example.aiagentdemo.tools;

import com.example.aiagentdemo.support.UserContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 提问历史工具：记录/查询均走 Redis List（key = ai:history:{user}，只保留最近 50 条）。 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HistoryTools {

    private static final String KEY_PREFIX = "ai:history:";
    private static final int KEEP_MAX = 50;
    private static final int QUERY_MAX = 20;

    private final StringRedisTemplate stringRedisTemplate;

    /** 记录一次提问（最新在前）；Redis 异常只告警，不影响问答主流程。 */
    public void recordQuestion(String question) {
        if (question == null || question.isBlank()) {
            return;
        }
        try {
            String key = KEY_PREFIX + UserContext.get();
            stringRedisTemplate.opsForList().leftPush(key, question.trim());
            stringRedisTemplate.opsForList().trim(key, 0, KEEP_MAX - 1);
        } catch (Exception e) {
            log.warn("记录提问历史失败（不影响问答）：{}", e.getMessage());
        }
    }

    @Tool(name = "query_question_history",
            description = "查询指定用户的历史提问记录（最新在前，最多20条）。"
                    + "当用户询问“我之前问过什么/我的提问历史/某用户的提问记录”时调用。")
    public Map<String, Object> queryQuestionHistory(
            @ToolParam(description = "要查询的用户名；查询当前提问者本人时可省略", required = false)
            String username) {
        String user = (username == null || username.isBlank()) ? UserContext.get() : username.trim();
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("user", user);
        try {
            List<String> history = stringRedisTemplate.opsForList().range(KEY_PREFIX + user, 0, QUERY_MAX - 1);
            result.put("count", history == null ? 0 : history.size());
            result.put("history", history == null ? List.of() : history);
        } catch (Exception e) {
            log.warn("查询提问历史失败：{}", e.getMessage());
            result.put("count", 0);
            result.put("history", List.of());
        }
        return result;
    }
}