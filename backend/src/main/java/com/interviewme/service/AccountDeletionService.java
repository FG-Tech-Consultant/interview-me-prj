package com.interviewme.service;

import com.interviewme.model.User;
import com.interviewme.repository.AccountDeletionRepository;
import com.interviewme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountDeletionService {

    private final AccountDeletionRepository deletionRepository;
    private final UserRepository userRepository;

    @Transactional
    public Map<String, Integer> deleteAccount(Long tenantId, Long userId) {
        log.info("Deleting account for tenant={}, user={}", tenantId, userId);

        Map<String, Integer> counts = new LinkedHashMap<>();

        // Delete in leaf-to-root FK order
        counts.put("storySkills", deletionRepository.deleteStorySkillsByTenantId(tenantId));
        counts.put("experienceProjectSkills", deletionRepository.deleteExperienceProjectSkillsByTenantId(tenantId));
        counts.put("stories", deletionRepository.deleteStoriesByTenantId(tenantId));
        counts.put("experienceProjects", deletionRepository.deleteExperienceProjectsByTenantId(tenantId));
        counts.put("chatMessages", deletionRepository.deleteChatMessagesByTenantId(tenantId));
        counts.put("chatSessions", deletionRepository.deleteChatSessionsByTenantId(tenantId));
        counts.put("linkedInSectionScores", deletionRepository.deleteLinkedInSectionScoresByTenantId(tenantId));
        counts.put("linkedInAnalyses", deletionRepository.deleteLinkedInAnalysesByTenantId(tenantId));
        counts.put("exportHistories", deletionRepository.deleteExportHistoriesByTenantId(tenantId));
        counts.put("contentEmbeddings", deletionRepository.deleteContentEmbeddingsByTenantId(tenantId));
        counts.put("userSkills", deletionRepository.deleteUserSkillsByTenantId(tenantId));
        counts.put("educations", deletionRepository.deleteEducationsByTenantId(tenantId));
        counts.put("jobExperiences", deletionRepository.deleteJobExperiencesByTenantId(tenantId));
        counts.put("coinTransactions", deletionRepository.deleteCoinTransactionsByTenantId(tenantId));
        counts.put("freeTierUsages", deletionRepository.deleteFreeTierUsagesByTenantId(tenantId));
        counts.put("coinWallets", deletionRepository.deleteCoinWalletsByTenantId(tenantId));
        counts.put("profiles", deletionRepository.deleteProfilesByTenantId(tenantId));
        counts.put("users", deletionRepository.deleteUsersByTenantId(tenantId));
        counts.put("tenants", deletionRepository.deleteTenantById(tenantId));

        log.info("Account deleted for tenant={}. Counts: {}", tenantId, counts);
        return counts;
    }

    @Transactional
    public int deleteAccountsByEmailPattern(String emailPattern) {
        List<User> users = userRepository.findByEmailLike(emailPattern);
        int count = 0;
        for (User user : users) {
            deleteAccount(user.getTenantId(), user.getId());
            count++;
        }
        return count;
    }
}
