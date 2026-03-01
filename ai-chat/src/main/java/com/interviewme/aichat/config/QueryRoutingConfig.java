package com.interviewme.aichat.config;

import com.interviewme.aichat.model.ContentType;
import com.interviewme.aichat.repository.ContentEmbeddingRepository;
import com.interviewme.aichat.service.ProfileContentRetriever;
import dev.langchain4j.model.ollama.OllamaChatModel;
import dev.langchain4j.model.ollama.OllamaEmbeddingModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.query.router.LanguageModelQueryRouter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
@Slf4j
public class QueryRoutingConfig {

    @Bean
    @ConditionalOnExpression("'${ai.ollama.base-url:}' != ''")
    public OllamaChatModel ollamaChatModel(AiProperties aiProperties) {
        log.info("Creating OllamaChatModel with base URL: {}", aiProperties.getOllama().getBaseUrl());
        return OllamaChatModel.builder()
                .baseUrl(aiProperties.getOllama().getBaseUrl())
                .modelName(aiProperties.getOllama().getChatModel())
                .build();
    }

    @Bean
    @ConditionalOnExpression("'${ai.ollama.base-url:}' != ''")
    public OllamaEmbeddingModel ollamaEmbeddingModel(AiProperties aiProperties) {
        log.info("Creating OllamaEmbeddingModel with base URL: {}", aiProperties.getOllama().getBaseUrl());
        return OllamaEmbeddingModel.builder()
                .baseUrl(aiProperties.getOllama().getBaseUrl())
                .modelName(aiProperties.getOllama().getEmbeddingModel())
                .build();
    }

    @Bean
    @ConditionalOnExpression("'${ai.ollama.base-url:}' != ''")
    public LanguageModelQueryRouter queryRouter(
            OllamaChatModel ollamaChatModel,
            OllamaEmbeddingModel ollamaEmbeddingModel,
            ContentEmbeddingRepository embeddingRepository,
            AiProperties aiProperties) {

        int topK = aiProperties.getEmbedding().getTopK();
        double threshold = aiProperties.getEmbedding().getSimilarityThreshold();

        Map<ContentRetriever, String> retrieverToDescription = new LinkedHashMap<>();

        retrieverToDescription.put(
                new ProfileContentRetriever(embeddingRepository, ollamaEmbeddingModel, ContentType.SKILL, topK, threshold),
                "technical skills, programming languages, frameworks, tools, and technologies the candidate knows");

        retrieverToDescription.put(
                new ProfileContentRetriever(embeddingRepository, ollamaEmbeddingModel, ContentType.STORY, topK, threshold),
                "personal stories, case studies, challenges overcome, STAR format narratives");

        retrieverToDescription.put(
                new ProfileContentRetriever(embeddingRepository, ollamaEmbeddingModel, ContentType.PROJECT, topK, threshold),
                "software projects, applications built, technical implementations and architectures");

        retrieverToDescription.put(
                new ProfileContentRetriever(embeddingRepository, ollamaEmbeddingModel, ContentType.JOB, topK, threshold),
                "work experience, job history, companies, roles, responsibilities, employment dates");

        retrieverToDescription.put(
                new ProfileContentRetriever(embeddingRepository, ollamaEmbeddingModel, ContentType.EDUCATION, topK, threshold),
                "education background, degrees, universities, certifications, courses, academic qualifications");

        retrieverToDescription.put(
                new ProfileContentRetriever(embeddingRepository, ollamaEmbeddingModel, ContentType.PROFILE_SUMMARY, topK, threshold),
                "professional summary, career overview, headline, location");

        retrieverToDescription.put(
                new ProfileContentRetriever(embeddingRepository, ollamaEmbeddingModel, ContentType.LANGUAGE, topK, threshold),
                "languages spoken, language proficiency, fluency levels, native language, multilingual abilities");

        log.info("Created LanguageModelQueryRouter with {} content type retrievers", retrieverToDescription.size());
        return new LanguageModelQueryRouter(ollamaChatModel, retrieverToDescription);
    }
}
