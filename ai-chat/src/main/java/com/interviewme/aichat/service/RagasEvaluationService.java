package com.interviewme.aichat.service;

import com.interviewme.aichat.client.LlmChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * RAGAS-inspired evaluation: scores RAG output quality on three dimensions:
 * - Faithfulness: is the answer grounded in the retrieved context?
 * - Relevance: does the context actually answer the question?
 * - Groundedness: are specific claims supported by specific context passages?
 *
 * Scores are logged for monitoring and can be exposed via API for dashboards.
 */
@Service
@Slf4j
public class RagasEvaluationService {

    private static final Pattern SCORE_PATTERN = Pattern.compile(
        "(faithfulness|relevance|groundedness)\\s*[:=]\\s*(\\d+(?:\\.\\d+)?)", Pattern.CASE_INSENSITIVE);

    private static final String SYSTEM_PROMPT = """
        You are a RAG quality evaluator. Given a question, the retrieved context, and the generated answer, \
        evaluate the quality on three dimensions (0.0 to 1.0 scale):

        1. faithfulness: Is the answer factually consistent with the context? (1.0 = fully faithful)
        2. relevance: Does the context contain information relevant to the question? (1.0 = highly relevant)
        3. groundedness: Are specific claims in the answer supported by specific passages in the context? (1.0 = fully grounded)

        Output ONLY the three scores in this exact format:
        faithfulness: 0.85
        relevance: 0.90
        groundedness: 0.80""";

    private final LlmRouterService llmRouter;

    public RagasEvaluationService(LlmRouterService llmRouter) {
        this.llmRouter = llmRouter;
    }

    /**
     * Evaluates RAG quality asynchronously. Returns scores map.
     * This is designed to be called after the response is generated,
     * not in the critical path.
     */
    public Map<String, Double> evaluate(Long tenantId, String question, String context, String answer) {
        Map<String, Double> scores = new HashMap<>();

        try {
            String userPrompt = "Question: " + question +
                "\n\nContext:\n" + truncate(context, 2000) +
                "\n\nAnswer:\n" + truncate(answer, 1000);

            var result = llmRouter.completeWithRequest(tenantId,
                SYSTEM_PROMPT,
                List.of(new LlmChatMessage("user", userPrompt)));

            scores = parseScores(result.response().content());

            log.info("RAGAS evaluation tenantId={} faithfulness={} relevance={} groundedness={}",
                tenantId,
                scores.getOrDefault("faithfulness", -1.0),
                scores.getOrDefault("relevance", -1.0),
                scores.getOrDefault("groundedness", -1.0));

        } catch (Exception e) {
            log.warn("RAGAS evaluation failed: {}", e.getMessage());
        }

        return scores;
    }

    static Map<String, Double> parseScores(String response) {
        Map<String, Double> scores = new HashMap<>();
        if (response == null) return scores;

        Matcher matcher = SCORE_PATTERN.matcher(response);
        while (matcher.find()) {
            String metric = matcher.group(1).toLowerCase();
            double score = Math.min(Double.parseDouble(matcher.group(2)), 1.0);
            scores.put(metric, score);
        }
        return scores;
    }

    private String truncate(String s, int maxLen) {
        if (s == null) return "";
        return s.length() > maxLen ? s.substring(0, maxLen) + "..." : s;
    }
}
