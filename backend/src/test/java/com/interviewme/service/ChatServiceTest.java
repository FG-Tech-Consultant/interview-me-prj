package com.interviewme.service;

import com.interviewme.aichat.client.LlmChatMessage;
import com.interviewme.aichat.client.LlmResponse;
import com.interviewme.aichat.config.AiProperties;
import com.interviewme.aichat.exception.ChatQuotaExceededException;
import com.interviewme.aichat.exception.ChatRateLimitException;
import com.interviewme.aichat.exception.LlmUnavailableException;
import com.interviewme.aichat.service.LlmRouterService;
import com.interviewme.aichat.service.RagService;
import com.interviewme.aichat.dto.*;
import com.interviewme.aichat.model.*;
import com.interviewme.aichat.repository.ChatMessageRepository;
import com.interviewme.aichat.repository.ChatSessionRepository;
import com.interviewme.billing.config.BillingProperties;
import com.interviewme.billing.dto.QuotaStatusResponse;
import com.interviewme.billing.exception.InsufficientBalanceException;
import com.interviewme.billing.model.CoinTransaction;
import com.interviewme.billing.model.FeatureType;
import com.interviewme.billing.model.RefType;
import com.interviewme.billing.service.CoinWalletService;
import com.interviewme.billing.service.FreeTierService;
import com.interviewme.common.exception.PublicProfileNotFoundException;
import com.interviewme.model.Profile;
import com.interviewme.repository.ProfileRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService")
class ChatServiceTest {

    @Mock private ChatSessionRepository sessionRepository;
    @Mock private ChatMessageRepository messageRepository;
    @Mock private ProfileRepository profileRepository;
    @Mock private RagService ragService;
    @Mock private LlmRouterService llmRouter;
    @Mock private FreeTierService freeTierService;
    @Mock private CoinWalletService coinWalletService;
    @Mock private BillingProperties billingProperties;
    @Mock private AiProperties aiProperties;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private ChatService chatService;

    private static final Long TENANT_ID = 100L;
    private static final Long PROFILE_ID = 10L;

    private Profile buildProfile() {
        Profile p = new Profile();
        p.setId(PROFILE_ID);
        p.setTenantId(TENANT_ID);
        p.setSlug("john-doe");
        p.setFullName("John Doe");
        p.setHeadline("Senior Developer");
        return p;
    }

    private ChatSession buildSession(UUID token) {
        ChatSession session = new ChatSession();
        session.setId(1L);
        session.setTenantId(TENANT_ID);
        session.setProfileId(PROFILE_ID);
        session.setSessionToken(token);
        session.setStatus(ChatSessionStatus.ACTIVE);
        session.setLastMessageAt(Instant.now());
        session.setMessageCount(0);
        return session;
    }

    private AiProperties.ChatConfig buildChatConfig() {
        AiProperties.ChatConfig config = new AiProperties.ChatConfig();
        config.setRateLimitPerMinute(5);
        config.setSessionExpiryHours(24);
        return config;
    }

    @Nested
    @DisplayName("processMessage")
    class ProcessMessage {

        @Test
        @DisplayName("processes message with free tier successfully")
        void processesWithFreeTier() {
            Profile profile = buildProfile();
            when(profileRepository.findBySlugAndDeletedAtIsNull("john-doe")).thenReturn(Optional.of(profile));

            UUID token = UUID.randomUUID();
            ChatSession session = buildSession(token);
            when(sessionRepository.findBySessionTokenAndStatus(token, ChatSessionStatus.ACTIVE))
                    .thenReturn(Optional.of(session));
            when(aiProperties.getChat()).thenReturn(buildChatConfig());

            // Rate limit check
            when(messageRepository.countBySessionIdAndRoleAndCreatedAtAfter(eq(1L), eq(ChatMessageRole.USER), any()))
                    .thenReturn(0L);

            // Free tier
            when(freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE)).thenReturn(true);

            // Save user message
            ChatMessage userMsg = new ChatMessage();
            userMsg.setId(100L);
            ChatMessage assistantMsg = new ChatMessage();
            assistantMsg.setId(101L);
            when(messageRepository.save(any(ChatMessage.class)))
                    .thenReturn(userMsg)
                    .thenReturn(assistantMsg)
                    .thenReturn(assistantMsg);

            // RAG
            when(ragService.retrieveContext(TENANT_ID, "Hello")).thenReturn("Context about John");

            // Conversation history
            when(messageRepository.findTop10BySessionIdOrderByCreatedAtDesc(1L))
                    .thenReturn(Collections.emptyList());

            // LLM
            LlmResponse llmResponse = new LlmResponse("Hello! I'm John's assistant.", 150, "openai", "gpt-4o-mini", 500L);
            when(llmRouter.complete(eq(TENANT_ID), anyString(), anyList())).thenReturn(llmResponse);

            when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);

            // Quota info
            QuotaStatusResponse quotaStatus = new QuotaStatusResponse("CHAT_MESSAGE", 1, 10, false, "2026-02");
            when(freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE)).thenReturn(quotaStatus);

            ChatRequest request = new ChatRequest("Hello", token);
            ChatResponse result = chatService.processMessage("john-doe", request);

            assertThat(result.message()).isEqualTo("Hello! I'm John's assistant.");
            assertThat(result.sessionToken()).isEqualTo(token);
            verify(coinWalletService, never()).spend(anyLong(), anyInt(), any(), anyString(), anyString());
        }

        @Test
        @DisplayName("spends coins when free tier exhausted")
        void spendsCoinsWhenFreeTierExhausted() {
            Profile profile = buildProfile();
            when(profileRepository.findBySlugAndDeletedAtIsNull("john-doe")).thenReturn(Optional.of(profile));

            UUID token = UUID.randomUUID();
            ChatSession session = buildSession(token);
            when(sessionRepository.findBySessionTokenAndStatus(token, ChatSessionStatus.ACTIVE))
                    .thenReturn(Optional.of(session));
            when(aiProperties.getChat()).thenReturn(buildChatConfig());
            when(messageRepository.countBySessionIdAndRoleAndCreatedAtAfter(eq(1L), eq(ChatMessageRole.USER), any()))
                    .thenReturn(0L);

            // Free tier exhausted
            when(freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE)).thenReturn(false);
            when(billingProperties.getCosts()).thenReturn(Map.of("CHAT_MESSAGE", 1));
            CoinTransaction coinTx = new CoinTransaction();
            coinTx.setId(999L);
            when(coinWalletService.spend(eq(TENANT_ID), eq(1), eq(RefType.CHAT_MESSAGE), anyString(), anyString()))
                    .thenReturn(coinTx);

            ChatMessage msg = new ChatMessage();
            msg.setId(100L);
            when(messageRepository.save(any(ChatMessage.class))).thenReturn(msg);
            when(ragService.retrieveContext(eq(TENANT_ID), anyString())).thenReturn("ctx");
            when(messageRepository.findTop10BySessionIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

            LlmResponse llmResponse = new LlmResponse("Hi", 100, "openai", "gpt-4o-mini", 300L);
            when(llmRouter.complete(eq(TENANT_ID), anyString(), anyList())).thenReturn(llmResponse);
            when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);

            QuotaStatusResponse quotaStatus = new QuotaStatusResponse("CHAT_MESSAGE", 10, 10, true, "2026-02");
            when(freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE)).thenReturn(quotaStatus);

            ChatRequest request = new ChatRequest("Hi", token);
            ChatResponse result = chatService.processMessage("john-doe", request);

            verify(coinWalletService).spend(eq(TENANT_ID), eq(1), eq(RefType.CHAT_MESSAGE), anyString(), anyString());
        }

        @Test
        @DisplayName("throws PublicProfileNotFoundException when slug not found")
        void throwsWhenSlugNotFound() {
            when(profileRepository.findBySlugAndDeletedAtIsNull("unknown")).thenReturn(Optional.empty());

            ChatRequest request = new ChatRequest("Hello", null);

            assertThatThrownBy(() -> chatService.processMessage("unknown", request))
                    .isInstanceOf(PublicProfileNotFoundException.class);
        }

        @Test
        @DisplayName("throws ChatQuotaExceededException when insufficient balance")
        void throwsWhenInsufficientBalance() {
            Profile profile = buildProfile();
            when(profileRepository.findBySlugAndDeletedAtIsNull("john-doe")).thenReturn(Optional.of(profile));

            ChatSession session = buildSession(null);
            when(sessionRepository.save(any(ChatSession.class))).thenReturn(session);
            when(aiProperties.getChat()).thenReturn(buildChatConfig());
            when(messageRepository.countBySessionIdAndRoleAndCreatedAtAfter(eq(1L), eq(ChatMessageRole.USER), any()))
                    .thenReturn(0L);

            when(freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE)).thenReturn(false);
            when(billingProperties.getCosts()).thenReturn(Map.of("CHAT_MESSAGE", 1));
            when(coinWalletService.spend(eq(TENANT_ID), eq(1), eq(RefType.CHAT_MESSAGE), anyString(), anyString()))
                    .thenThrow(new InsufficientBalanceException(1, 0L));

            ChatRequest request = new ChatRequest("Hello", null);

            assertThatThrownBy(() -> chatService.processMessage("john-doe", request))
                    .isInstanceOf(ChatQuotaExceededException.class);
        }

        @Test
        @DisplayName("throws ChatRateLimitException when rate limit exceeded")
        void throwsWhenRateLimitExceeded() {
            Profile profile = buildProfile();
            when(profileRepository.findBySlugAndDeletedAtIsNull("john-doe")).thenReturn(Optional.of(profile));

            UUID token = UUID.randomUUID();
            ChatSession session = buildSession(token);
            when(sessionRepository.findBySessionTokenAndStatus(token, ChatSessionStatus.ACTIVE))
                    .thenReturn(Optional.of(session));

            AiProperties.ChatConfig chatConfig = buildChatConfig();
            chatConfig.setRateLimitPerMinute(5);
            when(aiProperties.getChat()).thenReturn(chatConfig);
            when(messageRepository.countBySessionIdAndRoleAndCreatedAtAfter(eq(1L), eq(ChatMessageRole.USER), any()))
                    .thenReturn(5L);

            ChatRequest request = new ChatRequest("Hello", token);

            assertThatThrownBy(() -> chatService.processMessage("john-doe", request))
                    .isInstanceOf(ChatRateLimitException.class);
        }

        @Test
        @DisplayName("refunds coins on LLM failure")
        void refundsCoinsOnLlmFailure() {
            Profile profile = buildProfile();
            when(profileRepository.findBySlugAndDeletedAtIsNull("john-doe")).thenReturn(Optional.of(profile));

            UUID token = UUID.randomUUID();
            ChatSession session = buildSession(token);
            when(sessionRepository.findBySessionTokenAndStatus(token, ChatSessionStatus.ACTIVE))
                    .thenReturn(Optional.of(session));
            when(aiProperties.getChat()).thenReturn(buildChatConfig());
            when(messageRepository.countBySessionIdAndRoleAndCreatedAtAfter(eq(1L), eq(ChatMessageRole.USER), any()))
                    .thenReturn(0L);

            when(freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE)).thenReturn(false);
            when(billingProperties.getCosts()).thenReturn(Map.of("CHAT_MESSAGE", 1));
            CoinTransaction coinTx = new CoinTransaction();
            coinTx.setId(999L);
            when(coinWalletService.spend(eq(TENANT_ID), eq(1), eq(RefType.CHAT_MESSAGE), anyString(), anyString()))
                    .thenReturn(coinTx);

            ChatMessage msg = new ChatMessage();
            msg.setId(100L);
            when(messageRepository.save(any(ChatMessage.class))).thenReturn(msg);
            when(ragService.retrieveContext(eq(TENANT_ID), anyString())).thenReturn("ctx");
            when(messageRepository.findTop10BySessionIdOrderByCreatedAtDesc(1L)).thenReturn(Collections.emptyList());

            when(llmRouter.complete(eq(TENANT_ID), anyString(), anyList()))
                    .thenThrow(new LlmUnavailableException("Service down"));

            ChatRequest request = new ChatRequest("Hello", token);

            assertThatThrownBy(() -> chatService.processMessage("john-doe", request))
                    .isInstanceOf(LlmUnavailableException.class);

            verify(coinWalletService).refund(TENANT_ID, 999L);
        }
    }

    @Nested
    @DisplayName("getAnalytics")
    class GetAnalytics {

        @Test
        @DisplayName("returns analytics for tenant")
        void returnsAnalytics() {
            when(sessionRepository.countByTenantIdAndCreatedAtAfter(eq(TENANT_ID), any())).thenReturn(25L);
            when(messageRepository.countByTenantId(TENANT_ID)).thenReturn(150L);
            when(sessionRepository.countByTenantIdAndCreatedAtBetween(eq(TENANT_ID), any(), any())).thenReturn(5L);
            when(messageRepository.countByTenantIdAndCreatedAtBetween(eq(TENANT_ID), any(), any())).thenReturn(30L);

            ChatAnalyticsResponse result = chatService.getAnalytics(TENANT_ID);

            assertThat(result.totalSessions()).isEqualTo(25);
            assertThat(result.totalMessages()).isEqualTo(150);
            assertThat(result.sessionsThisMonth()).isEqualTo(5);
            assertThat(result.messagesThisMonth()).isEqualTo(30);
        }
    }
}
