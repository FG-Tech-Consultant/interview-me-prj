package com.interviewme.aichat.service;

import com.interviewme.aichat.client.LlmChatMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Multi-query expansion: generates multiple query perspectives from a single
 * user query using the LLM. Each perspective captures different facets of the
 * question, improving recall for RAG retrieval.
 */
@Service
@Slf4j
public class MultiQueryExpander {

    private static final int MAX_EXPANSIONS = 3;
    private static final String SYSTEM_PROMPT = """
        You are a query expansion assistant. Given a user query, generate exactly 3 alternative \
        versions of the query that capture different perspectives or aspects. \
        Each version should help find relevant documents that the original query might miss. \
        Output ONLY the queries, one per line, numbered 1-3. Do not include explanations.""";

    private final LlmRouterService llmRouter;

    public MultiQueryExpander(LlmRouterService llmRouter) {
        this.llmRouter = llmRouter;
    }

    /**
     * Expands a user query into multiple perspective queries.
     * Returns the original query plus expanded variants.
     */
    public List<String> expand(Long tenantId, String originalQuery) {
        List<String> queries = new ArrayList<>();
        queries.add(originalQuery);

        try {
            var result = llmRouter.completeWithRequest(tenantId,
                SYSTEM_PROMPT,
                List.of(new LlmChatMessage("user", "Query: " + originalQuery)));

            String content = result.response().content();
            List<String> expanded = parseQueries(content);

            queries.addAll(expanded.stream().limit(MAX_EXPANSIONS).collect(Collectors.toList()));
            log.info("Multi-query expansion: original='{}' expanded={}",
                truncate(originalQuery), queries.size() - 1);
        } catch (Exception e) {
            log.warn("Multi-query expansion failed, using original only: {}", e.getMessage());
        }

        return queries;
    }

    static List<String> parseQueries(String response) {
        return Arrays.stream(response.split("\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .map(line -> line.replaceFirst("^\\d+[.):\\-]?\\s*", ""))
            .filter(line -> !line.isEmpty() && line.length() > 5)
            .collect(Collectors.toList());
    }

    private String truncate(String s) {
        return s.length() > 50 ? s.substring(0, 50) + "..." : s;
    }
}
