package com.interviewme.config;

import com.interviewme.aichat.client.LlmChatMessage;
import com.interviewme.aichat.service.LlmRouterService;
import com.interviewme.common.dto.ai.LlmClient;
import com.interviewme.common.dto.ai.LlmRequest;
import com.interviewme.common.dto.ai.LlmResponse;
import com.interviewme.security.TenantContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Adapter that bridges the common LlmClient interface to the ai-chat LlmRouterService.
 */
@Component
@RequiredArgsConstructor
public class LlmClientAdapter implements LlmClient {

    private final LlmRouterService llmRouterService;

    @Override
    public LlmResponse complete(LlmRequest request) {
        Long tenantId = TenantContext.getTenantId();

        List<LlmChatMessage> messages = List.of(
                new LlmChatMessage("user", request.prompt())
        );

        com.interviewme.aichat.client.LlmResponse routerResponse =
                llmRouterService.complete(tenantId, "You are a helpful AI assistant.", messages);

        return new LlmResponse(
                routerResponse.content(),
                routerResponse.tokensUsed(),
                routerResponse.latencyMs()
        );
    }
}
