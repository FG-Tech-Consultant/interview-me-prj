package com.interviewme.billing.service;

import com.interviewme.billing.dto.QuotaStatusResponse;
import com.interviewme.billing.model.FeatureType;
import com.interviewme.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class FreeTierServiceTest {

    @Autowired
    private FreeTierService freeTierService;

    private static final Long TENANT_ID = 1L;

    @BeforeEach
    void setUp() {
        TenantContext.setTenantId(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Nested
    @DisplayName("tryConsumeFreeTier")
    class TryConsumeFreeTier {

        @Test
        @DisplayName("should return true when under quota")
        void shouldReturnTrueWhenUnderQuota() {
            boolean consumed = freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);

            assertThat(consumed).isTrue();
        }

        @Test
        @DisplayName("should return false when at quota for chat messages")
        void shouldReturnFalseWhenAtQuota() {
            // Consume all 50 free chat messages
            for (int i = 0; i < 50; i++) {
                boolean result = freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);
                assertThat(result).isTrue();
            }

            // 51st should fail
            boolean consumed = freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);
            assertThat(consumed).isFalse();
        }

        @Test
        @DisplayName("should track different features independently")
        void shouldTrackDifferentFeaturesIndependently() {
            freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);
            freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.LINKEDIN_DRAFT);

            QuotaStatusResponse chatStatus = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE);
            QuotaStatusResponse linkedinStatus = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.LINKEDIN_DRAFT);

            assertThat(chatStatus.used()).isEqualTo(1);
            assertThat(linkedinStatus.used()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return false when at quota for LinkedIn drafts")
        void shouldReturnFalseWhenLinkedinDraftQuotaExhausted() {
            // LinkedIn draft quota is 10
            for (int i = 0; i < 10; i++) {
                boolean result = freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.LINKEDIN_DRAFT);
                assertThat(result).isTrue();
            }

            boolean consumed = freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.LINKEDIN_DRAFT);
            assertThat(consumed).isFalse();
        }

        @Test
        @DisplayName("should return false when at quota for LinkedIn suggestions")
        void shouldReturnFalseWhenLinkedinSuggestionQuotaExhausted() {
            // LinkedIn suggestion quota is 1
            boolean first = freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.LINKEDIN_SUGGESTION);
            assertThat(first).isTrue();

            boolean second = freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.LINKEDIN_SUGGESTION);
            assertThat(second).isFalse();
        }

        @Test
        @DisplayName("should track separate tenants independently")
        void shouldTrackTenantsIndependently() {
            freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);
            freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);

            Long otherTenant = 999L;
            freeTierService.tryConsumeFreeTier(otherTenant, FeatureType.CHAT_MESSAGE);

            QuotaStatusResponse status1 = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE);
            QuotaStatusResponse status2 = freeTierService.getQuotaStatus(otherTenant, FeatureType.CHAT_MESSAGE);

            assertThat(status1.used()).isEqualTo(2);
            assertThat(status2.used()).isEqualTo(1);
        }

        @Test
        @DisplayName("should increment usage count by 1 each call")
        void shouldIncrementByOne() {
            for (int i = 1; i <= 5; i++) {
                freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);
                QuotaStatusResponse status = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE);
                assertThat(status.used()).isEqualTo(i);
            }
        }
    }

    @Nested
    @DisplayName("getQuotaStatus")
    class GetQuotaStatus {

        @Test
        @DisplayName("should return correct usage and limit")
        void shouldReturnCorrectUsage() {
            freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);
            freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);

            QuotaStatusResponse status = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE);

            assertThat(status.used()).isEqualTo(2);
            assertThat(status.limit()).isEqualTo(50);
            assertThat(status.quotaExceeded()).isFalse();
            assertThat(status.featureType()).isEqualTo("CHAT_MESSAGE");
        }

        @Test
        @DisplayName("should return zero used for new feature")
        void shouldReturnZeroUsedForNewFeature() {
            QuotaStatusResponse status = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.LINKEDIN_DRAFT);

            assertThat(status.used()).isEqualTo(0);
            assertThat(status.limit()).isEqualTo(10);
            assertThat(status.quotaExceeded()).isFalse();
        }

        @Test
        @DisplayName("should report quota exceeded when at limit")
        void shouldReportQuotaExceeded() {
            // Exhaust the LinkedIn suggestion quota (1)
            freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.LINKEDIN_SUGGESTION);

            QuotaStatusResponse status = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.LINKEDIN_SUGGESTION);

            assertThat(status.quotaExceeded()).isTrue();
            assertThat(status.used()).isEqualTo(1);
            assertThat(status.limit()).isEqualTo(1);
        }

        @Test
        @DisplayName("should return correct limit for each feature type")
        void shouldReturnCorrectLimitPerFeature() {
            QuotaStatusResponse chatStatus = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE);
            QuotaStatusResponse linkedinStatus = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.LINKEDIN_DRAFT);
            QuotaStatusResponse suggestionStatus = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.LINKEDIN_SUGGESTION);

            assertThat(chatStatus.limit()).isEqualTo(50);
            assertThat(linkedinStatus.limit()).isEqualTo(10);
            assertThat(suggestionStatus.limit()).isEqualTo(1);
        }

        @Test
        @DisplayName("should include yearMonth in response")
        void shouldIncludeYearMonth() {
            QuotaStatusResponse status = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE);

            assertThat(status.yearMonth()).isNotNull();
            assertThat(status.yearMonth()).matches("\\d{4}-\\d{2}");
        }
    }
}
