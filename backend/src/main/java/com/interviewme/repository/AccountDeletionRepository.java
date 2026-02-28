package com.interviewme.repository;

import com.interviewme.model.Tenant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AccountDeletionRepository extends JpaRepository<Tenant, Long> {

    @Modifying
    @Query(value = "DELETE FROM package_story WHERE tenant_id = :tenantId", nativeQuery = true)
    int deletePackageStoriesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM package_project WHERE tenant_id = :tenantId", nativeQuery = true)
    int deletePackageProjectsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM package_skill WHERE tenant_id = :tenantId", nativeQuery = true)
    int deletePackageSkillsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM content_package WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteContentPackagesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM story_skill WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteStorySkillsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM experience_project_skill WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteExperienceProjectSkillsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM story WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteStoriesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM experience_project WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteExperienceProjectsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM chat_message WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteChatMessagesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM chat_session WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteChatSessionsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM linkedin_section_score WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteLinkedInSectionScoresByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM linkedin_analysis WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteLinkedInAnalysesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM export_history WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteExportHistoriesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM content_embedding WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteContentEmbeddingsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM user_skill WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteUserSkillsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM education WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteEducationsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM job_experience WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteJobExperiencesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM coin_transaction WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteCoinTransactionsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM free_tier_usage WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteFreeTierUsagesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM coin_wallet WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteCoinWalletsByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM profile WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteProfilesByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM users WHERE tenant_id = :tenantId", nativeQuery = true)
    int deleteUsersByTenantId(@Param("tenantId") Long tenantId);

    @Modifying
    @Query(value = "DELETE FROM tenant WHERE id = :tenantId", nativeQuery = true)
    int deleteTenantById(@Param("tenantId") Long tenantId);
}
