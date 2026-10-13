package com.example.aiagentdemo.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
public class AiService {

    @Value("${spring.ai.dashscope.api-key:${DASHSCOPE_API_KEY:}}")
    private String apiKey;

    private final ChatClient chatClient;
    private final EmbeddingModel embeddingModel;
    private final StringRedisTemplate stringRedisTemplate;

    public AiService(ChatClient.Builder chatClientBuilder,
                     EmbeddingModel embeddingModel,
                     StringRedisTemplate stringRedisTemplate) {
        this.chatClient = chatClientBuilder.build();
        this.embeddingModel = embeddingModel;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public String askQuestion(String userQuestion) {
        if (userQuestion == null || userQuestion.trim().isEmpty()) {
            return "请输入有效的问题。";
        }

        String cacheKey = "ai:qa:" + userQuestion.trim();
        String cachedAnswer = stringRedisTemplate.opsForValue().get(cacheKey);
        if (cachedAnswer != null && !cachedAnswer.isEmpty()) {
            System.out.println("【Redis 缓存命中】直接返回缓存结果");
            return cachedAnswer;
        }

        // 强化 Key 校验
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.contains("CHANGE_ME") || apiKey.startsWith("sk-") == false) {
            System.err.println("【校验失败】API Key 未配置或格式错误，请检查环境变量 DASHSCOPE_API_KEY 和 IDEA 重启状态！");
            return "人工客服话术：当前 AI 服务未配置密钥，请联系管理员。";
        }

        try {
            System.out.println("【调用大模型】提问：" + userQuestion);
            String aiAnswer = chatClient
                    .prompt()
                    .user(userQuestion)
                    .call()
                    .content();

            if (aiAnswer != null && !aiAnswer.isEmpty()) {
                stringRedisTemplate.opsForValue().set(cacheKey, aiAnswer, 1, TimeUnit.DAYS);
            }
            return aiAnswer;

        } catch (Exception e) {
            System.err.println("【AI 服务异常】" + e.getMessage());
            // 打印真实异常原因，方便排查
            if (e.getCause() != null) {
                System.err.println("【真实原因】" + e.getCause().getMessage());
            }
            return "人工客服话术：AI 服务暂时繁忙，请稍后再试。";
        }
    }

    public float[] getEmbedding(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("待向量化的文本不能为空");
        }
        try {
            return embeddingModel.embed(text);
        } catch (Exception e) {
            System.err.println("向量化失败：" + e.getMessage());
            throw new RuntimeException("文本向量化失败，请检查 Embedding 模型配置", e);
        }
    }
}

