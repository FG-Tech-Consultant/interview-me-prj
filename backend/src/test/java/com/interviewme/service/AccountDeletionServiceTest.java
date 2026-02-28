package com.interviewme.service;

import com.interviewme.model.User;
import com.interviewme.repository.AccountDeletionRepository;
import com.interviewme.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountDeletionService")
class AccountDeletionServiceTest {

    @Mock private AccountDeletionRepository deletionRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private AccountDeletionService accountDeletionService;

    @Nested
    @DisplayName("deleteAccount")
    class DeleteAccount {

        @Test
        @DisplayName("deletes all data in correct FK order and returns counts")
        void deletesAllDataInOrder() {
            Long tenantId = 100L;
            Long userId = 1L;

            when(deletionRepository.deletePackageStoriesByTenantId(tenantId)).thenReturn(2);
            when(deletionRepository.deletePackageProjectsByTenantId(tenantId)).thenReturn(1);
            when(deletionRepository.deletePackageSkillsByTenantId(tenantId)).thenReturn(3);
            when(deletionRepository.deleteContentPackagesByTenantId(tenantId)).thenReturn(1);
            when(deletionRepository.deleteStorySkillsByTenantId(tenantId)).thenReturn(5);
            when(deletionRepository.deleteExperienceProjectSkillsByTenantId(tenantId)).thenReturn(4);
            when(deletionRepository.deleteStoriesByTenantId(tenantId)).thenReturn(3);
            when(deletionRepository.deleteExperienceProjectsByTenantId(tenantId)).thenReturn(2);
            when(deletionRepository.deleteChatMessagesByTenantId(tenantId)).thenReturn(10);
            when(deletionRepository.deleteChatSessionsByTenantId(tenantId)).thenReturn(2);
            when(deletionRepository.deleteLinkedInSectionScoresByTenantId(tenantId)).thenReturn(6);
            when(deletionRepository.deleteLinkedInAnalysesByTenantId(tenantId)).thenReturn(1);
            when(deletionRepository.deleteExportHistoriesByTenantId(tenantId)).thenReturn(3);
            when(deletionRepository.deleteContentEmbeddingsByTenantId(tenantId)).thenReturn(8);
            when(deletionRepository.deleteUserSkillsByTenantId(tenantId)).thenReturn(7);
            when(deletionRepository.deleteEducationsByTenantId(tenantId)).thenReturn(2);
            when(deletionRepository.deleteJobExperiencesByTenantId(tenantId)).thenReturn(3);
            when(deletionRepository.deleteCoinTransactionsByTenantId(tenantId)).thenReturn(5);
            when(deletionRepository.deleteFreeTierUsagesByTenantId(tenantId)).thenReturn(4);
            when(deletionRepository.deleteCoinWalletsByTenantId(tenantId)).thenReturn(1);
            when(deletionRepository.deleteProfilesByTenantId(tenantId)).thenReturn(1);
            when(deletionRepository.deleteUsersByTenantId(tenantId)).thenReturn(1);
            when(deletionRepository.deleteTenantById(tenantId)).thenReturn(1);

            Map<String, Integer> counts = accountDeletionService.deleteAccount(tenantId, userId);

            assertThat(counts).hasSize(24);
            assertThat(counts.get("packageStories")).isEqualTo(2);
            assertThat(counts.get("tenants")).isEqualTo(1);
            assertThat(counts.get("chatMessages")).isEqualTo(10);

            // Verify leaf tables deleted before root tables
            var inOrder = inOrder(deletionRepository);
            inOrder.verify(deletionRepository).deletePackageStoriesByTenantId(tenantId);
            inOrder.verify(deletionRepository).deleteProfilesByTenantId(tenantId);
            inOrder.verify(deletionRepository).deleteUsersByTenantId(tenantId);
            inOrder.verify(deletionRepository).deleteTenantById(tenantId);
        }

        @Test
        @DisplayName("handles zero counts for empty account")
        void handlesEmptyAccount() {
            Long tenantId = 200L;

            // All return 0
            when(deletionRepository.deletePackageStoriesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deletePackageProjectsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deletePackageSkillsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteContentPackagesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteStorySkillsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteExperienceProjectSkillsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteStoriesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteExperienceProjectsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteChatMessagesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteChatSessionsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteLinkedInDraftsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteLinkedInSectionScoresByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteLinkedInAnalysesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteExportHistoriesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteContentEmbeddingsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteUserSkillsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteEducationsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteJobExperiencesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteCoinTransactionsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteFreeTierUsagesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteCoinWalletsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteProfilesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteUsersByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteTenantById(tenantId)).thenReturn(0);

            Map<String, Integer> counts = accountDeletionService.deleteAccount(tenantId, 1L);

            assertThat(counts.values()).allMatch(v -> v == 0);
        }
    }

    @Nested
    @DisplayName("deleteAccountsByEmailPattern")
    class DeleteAccountsByEmailPattern {

        @Test
        @DisplayName("deletes multiple accounts matching email pattern")
        void deletesMultipleAccounts() {
            User u1 = new User();
            u1.setId(1L);
            u1.setTenantId(100L);
            User u2 = new User();
            u2.setId(2L);
            u2.setTenantId(200L);

            when(userRepository.findByEmailLike("%test%")).thenReturn(List.of(u1, u2));

            // Stub all deletion methods for both tenants
            stubAllDeletions(100L);
            stubAllDeletions(200L);

            int count = accountDeletionService.deleteAccountsByEmailPattern("%test%");

            assertThat(count).isEqualTo(2);
        }

        @Test
        @DisplayName("returns zero when no users match")
        void returnsZeroWhenNoMatch() {
            when(userRepository.findByEmailLike("%nonexistent%")).thenReturn(List.of());

            int count = accountDeletionService.deleteAccountsByEmailPattern("%nonexistent%");

            assertThat(count).isZero();
        }

        private void stubAllDeletions(Long tenantId) {
            when(deletionRepository.deletePackageStoriesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deletePackageProjectsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deletePackageSkillsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteContentPackagesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteStorySkillsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteExperienceProjectSkillsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteStoriesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteExperienceProjectsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteChatMessagesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteChatSessionsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteLinkedInDraftsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteLinkedInSectionScoresByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteLinkedInAnalysesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteExportHistoriesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteContentEmbeddingsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteUserSkillsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteEducationsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteJobExperiencesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteCoinTransactionsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteFreeTierUsagesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteCoinWalletsByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteProfilesByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteUsersByTenantId(tenantId)).thenReturn(0);
            when(deletionRepository.deleteTenantById(tenantId)).thenReturn(0);
        }
    }
}
