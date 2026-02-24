package com.interviewme.billing.service;

import com.interviewme.billing.dto.QuotaStatusResponse;
import com.interviewme.billing.model.FeatureType;
import com.interviewme.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
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

    @Test
    void tryConsumeFreeTier_shouldReturnTrueWhenUnderQuota() {
        boolean consumed = freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);

        assertThat(consumed).isTrue();
    }

    @Test
    void tryConsumeFreeTier_shouldReturnFalseWhenAtQuota() {
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
    void getQuotaStatus_shouldReturnCorrectUsage() {
        freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);
        freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);

        QuotaStatusResponse status = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE);

        assertThat(status.used()).isEqualTo(2);
        assertThat(status.limit()).isEqualTo(50);
        assertThat(status.quotaExceeded()).isFalse();
        assertThat(status.featureType()).isEqualTo("CHAT_MESSAGE");
    }

    @Test
    void tryConsumeFreeTier_shouldTrackDifferentFeaturesIndependently() {
        freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.CHAT_MESSAGE);
        freeTierService.tryConsumeFreeTier(TENANT_ID, FeatureType.LINKEDIN_DRAFT);

        QuotaStatusResponse chatStatus = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.CHAT_MESSAGE);
        QuotaStatusResponse linkedinStatus = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.LINKEDIN_DRAFT);

        assertThat(chatStatus.used()).isEqualTo(1);
        assertThat(linkedinStatus.used()).isEqualTo(1);
    }

    @Test
    void getQuotaStatus_shouldReturnZeroUsedForNewFeature() {
        QuotaStatusResponse status = freeTierService.getQuotaStatus(TENANT_ID, FeatureType.LINKEDIN_DRAFT);

        assertThat(status.used()).isEqualTo(0);
        assertThat(status.limit()).isEqualTo(10);
        assertThat(status.quotaExceeded()).isFalse();
    }
}
