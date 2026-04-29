package com.interviewme.aichat.service;

import com.interviewme.aichat.client.LlmChatMessage;
import com.interviewme.aichat.service.HybridRetrievalService.RetrievedDocument;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * LLM-based re-ranking: scores each retrieved document's relevance to the query
 * using the LLM as a cross-encoder substitute. Only applied to top-K candidates
 * from hybrid retrieval to keep costs low.
 */
@Service
@Slf4j
public class ReRankingService {

    private static final int RERANK_BATCH_SIZE = 10;
    private static final Pattern SCORE_PATTERN = Pattern.compile("\\[(\\d+)]\\s*[:=]?\\s*(\\d+(?:\\.\\d+)?)");

    private final LlmRouterService llmRouter;

    public ReRankingService(LlmRouterService llmRouter) {
        this.llmRouter = llmRouter;
    }

    /**
     * Re-ranks documents using LLM scoring. Returns documents sorted by relevance.
     * Falls back to original RRF ordering if LLM call fails.
     */
    public List<RetrievedDocument> reRank(Long tenantId, String query, List<RetrievedDocument> candidates, int topK) {
        if (candidates.size() <= 1) {
            return candidates;
        }

        // Take at most RERANK_BATCH_SIZE for LLM scoring
        List<RetrievedDocument> toRerank = candidates.stream()
            .limit(RERANK_BATCH_SIZE)
            .collect(Collectors.toList());

        try {
            Map<Integer, Double> scores = scoreWithLlm(tenantId, query, toRerank);

            // Sort by LLM score descending
            List<RetrievedDocument> reranked = new ArrayList<>(toRerank);
            reranked.sort((a, b) -> {
                int idxA = toRerank.indexOf(a);
                int idxB = toRerank.indexOf(b);
                double scoreA = scores.getOrDefault(idxA, 0.0);
                double scoreB = scores.getOrDefault(idxB, 0.0);
                return Double.compare(scoreB, scoreA);
            });

            // Append any remaining candidates (beyond RERANK_BATCH_SIZE) at the end
            if (candidates.size() > RERANK_BATCH_SIZE) {
                reranked.addAll(candidates.subList(RERANK_BATCH_SIZE, candidates.size()));
            }

            log.info("Re-ranked {} documents for tenantId={}, returning top {}", toRerank.size(), tenantId, topK);
            return reranked.stream().limit(topK).collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Re-ranking failed, using RRF order: {}", e.getMessage());
            return candidates.stream().limit(topK).collect(Collectors.toList());
        }
    }

    private Map<Integer, Double> scoreWithLlm(Long tenantId, String query, List<RetrievedDocument> docs) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Rate the relevance of each document to the query on a scale of 0-10.\n");
        prompt.append("Query: ").append(query).append("\n\n");

        for (int i = 0; i < docs.size(); i++) {
            String text = docs.get(i).getText();
            if (text.length() > 500) {
                text = text.substring(0, 500) + "...";
            }
            prompt.append("[").append(i).append("] ").append(text).append("\n\n");
        }

        prompt.append("Respond with ONLY the scores in format: [index]: score\n");
        prompt.append("Example: [0]: 8.5\n[1]: 3.2\n");

        var result = llmRouter.completeWithRequest(tenantId, 
            "You are a relevance scoring system. Output only scores, nothing else.",
            List.of(new LlmChatMessage("user", prompt.toString())));

        return parseScores(result.response().content());
    }

    static Map<Integer, Double> parseScores(String response) {
        Map<Integer, Double> scores = new HashMap<>();
        Matcher matcher = SCORE_PATTERN.matcher(response);
        while (matcher.find()) {
            int index = Integer.parseInt(matcher.group(1));
            double score = Double.parseDouble(matcher.group(2));
            scores.put(index, Math.min(score, 10.0));
        }
        return scores;
    }
}
