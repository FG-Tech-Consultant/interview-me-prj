package com.interviewme.aichat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "ai")
@Data
public class AiProperties {

    private String defaultProvider = "openai";
    private OpenAiConfig openai = new OpenAiConfig();
    private GeminiConfig gemini = new GeminiConfig();
    private ClaudeConfig claude = new ClaudeConfig();
    private OllamaConfig ollama = new OllamaConfig();
    private ChatConfig chat = new ChatConfig();
    private EmbeddingConfig embedding = new EmbeddingConfig();

    @Data
    public static class OpenAiConfig {
        private String apiKey;
        private String chatModel = "gpt-4o-mini";
        private String embeddingModel = "text-embedding-3-small";
        private String baseUrl = "https://api.openai.com/v1";
    }

    @Data
    public static class GeminiConfig {
        private String apiKey;
        private String chatModel = "gemini-2.0-flash";
        private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    }

    @Data
    public static class ClaudeConfig {
        private String apiKey;
        private String chatModel = "claude-sonnet-4-6";
        private String baseUrl = "https://api.anthropic.com/v1";
    }

    @Data
    public static class OllamaConfig {
        private String baseUrl;
        private String chatModel = "qwen2.5:3b";
        private String embeddingModel = "nomic-embed-text";
    }

    @Data
    public static class ChatConfig {
        private int maxTokens = 800;
        private double temperature = 0.3;
        private int maxContextMessages = 10;
        private int rateLimitPerMinute = 5;
        private int sessionExpiryHours = 24;
    }

    @Data
    public static class EmbeddingConfig {
        private String provider;
        private int dimension = 768;
        private double similarityThreshold = 0.25;
        private int topK = 10;
    }
}
