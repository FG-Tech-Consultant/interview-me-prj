package com.interviewme.billing.service;

import com.interviewme.billing.config.BillingProperties;
import com.interviewme.billing.dto.QuotaStatusResponse;
import com.interviewme.billing.model.FeatureType;
import com.interviewme.billing.model.FreeTierUsage;
import com.interviewme.billing.repository.FreeTierUsageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class FreeTierService {

    private final FreeTierUsageRepository usageRepository;
    private final BillingProperties billingProperties;

    private static final DateTimeFormatter YEAR_MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");

    @Transactional(readOnly = true)
    public QuotaStatusResponse getQuotaStatus(Long tenantId, FeatureType featureType) {
        String yearMonth = getCurrentYearMonth();
        int limit = getQuotaLimit(featureType);

        int used = usageRepository.findByTenantIdAndFeatureTypeAndYearMonth(tenantId, featureType, yearMonth)
                .map(FreeTierUsage::getUsageCount)
                .orElse(0);

        log.debug("Quota status: tenantId={}, feature={}, used={}/{}, month={}",
                  tenantId, featureType, used, limit, yearMonth);

        return new QuotaStatusResponse(
                featureType.name(),
                used,
                limit,
                used >= limit,
                yearMonth
        );
    }

    @Transactional
    public boolean tryConsumeFreeTier(Long tenantId, FeatureType featureType) {
        String yearMonth = getCurrentYearMonth();
        int limit = getQuotaLimit(featureType);

        FreeTierUsage usage = usageRepository
                .findByTenantIdAndFeatureTypeAndYearMonth(tenantId, featureType, yearMonth)
                .orElseGet(() -> {
                    FreeTierUsage newUsage = new FreeTierUsage();
                    newUsage.setTenantId(tenantId);
                    newUsage.setFeatureType(featureType);
                    newUsage.setYearMonth(yearMonth);
                    newUsage.setUsageCount(0);
                    return usageRepository.save(newUsage);
                });

        if (usage.getUsageCount() >= limit) {
            log.debug("Free tier quota exhausted: tenantId={}, feature={}, used={}/{}",
                      tenantId, featureType, usage.getUsageCount(), limit);
            return false;
        }

        usage.setUsageCount(usage.getUsageCount() + 1);
        usageRepository.save(usage);

        log.debug("Free tier consumed: tenantId={}, feature={}, used={}/{}",
                  tenantId, featureType, usage.getUsageCount(), limit);
        return true;
    }

    String getCurrentYearMonth() {
        return LocalDate.now().format(YEAR_MONTH_FORMAT);
    }

    private int getQuotaLimit(FeatureType featureType) {
        return switch (featureType) {
            case CHAT_MESSAGE -> billingProperties.getFreeTier().getChatMessagesPerMonth();
            case LINKEDIN_DRAFT -> billingProperties.getFreeTier().getLinkedinDraftsPerMonth();
            case LINKEDIN_SUGGESTION -> 1; // 1 free suggestion per section
        };
    }
}
