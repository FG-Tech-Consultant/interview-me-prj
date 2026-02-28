package com.interviewme.config;

import com.interviewme.aichat.config.AiProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AppInfoContributor implements InfoContributor {

    private final AiProperties aiProperties;

    @Override
    public void contribute(Info.Builder builder) {
        // LLM info
        Map<String, Object> llm = new LinkedHashMap<>();
        String provider = aiProperties.getDefaultProvider();
        llm.put("provider", provider);
        llm.put("model", resolveModel(provider));
        builder.withDetail("llm", llm);

        // OS info
        Map<String, Object> os = new LinkedHashMap<>();
        os.put("name", System.getProperty("os.name"));
        os.put("version", System.getProperty("os.version"));
        os.put("arch", System.getProperty("os.arch"));
        builder.withDetail("os", os);

        // Java info
        Map<String, Object> java = new LinkedHashMap<>();
        java.put("version", System.getProperty("java.version"));
        java.put("vendor", System.getProperty("java.vendor"));
        builder.withDetail("java", java);
    }

    private String resolveModel(String provider) {
        return switch (provider.toLowerCase()) {
            case "openai" -> aiProperties.getOpenai().getChatModel();
            case "gemini" -> aiProperties.getGemini().getChatModel();
            case "claude" -> aiProperties.getClaude().getChatModel();
            case "ollama" -> aiProperties.getOllama().getChatModel();
            default -> "unknown";
        };
    }
}
