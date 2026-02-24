package com.interviewme.billing.repository;

import com.interviewme.billing.model.FeatureType;
import com.interviewme.billing.model.FreeTierUsage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FreeTierUsageRepository extends JpaRepository<FreeTierUsage, Long> {

    Optional<FreeTierUsage> findByTenantIdAndFeatureTypeAndYearMonth(
            Long tenantId, FeatureType featureType, String yearMonth);
}
