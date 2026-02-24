package com.interviewme.profile.repository;

import com.interviewme.profile.model.Profile;
import com.interviewme.security.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class ProfileRepositoryTenantIsolationTest {

    @Autowired
    private ProfileRepository profileRepository;

    @Autowired
    private TestEntityManager entityManager;

    private static final Long TENANT_1 = 1L;
    private static final Long TENANT_2 = 2L;

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    void findByIdAndTenantId_OnlyReturnsSameTenantProfile() {
        // Given
        Profile tenant1Profile = new Profile();
        tenant1Profile.setUserId(100L);
        tenant1Profile.setTenantId(TENANT_1);
        tenant1Profile.setFullName("Tenant 1 User");
        tenant1Profile.setHeadline("Developer");
        tenant1Profile = entityManager.persist(tenant1Profile);

        Profile tenant2Profile = new Profile();
        tenant2Profile.setUserId(101L);
        tenant2Profile.setTenantId(TENANT_2);
        tenant2Profile.setFullName("Tenant 2 User");
        tenant2Profile.setHeadline("Engineer");
        tenant2Profile = entityManager.persist(tenant2Profile);

        entityManager.flush();
        entityManager.clear();

        // When - Try to access Tenant 2's profile with Tenant 1's ID
        Optional<Profile> wrongTenantResult = profileRepository.findByIdAndTenantId(
                tenant2Profile.getId(), TENANT_1
        );

        // Then - Should not find the profile
        assertThat(wrongTenantResult).isEmpty();

        // When - Access correct tenant's profile
        Optional<Profile> correctTenantResult = profileRepository.findByIdAndTenantId(
                tenant1Profile.getId(), TENANT_1
        );

        // Then - Should find the profile
        assertThat(correctTenantResult).isPresent();
        assertThat(correctTenantResult.get().getFullName()).isEqualTo("Tenant 1 User");
    }

    @Test
    void existsByUserIdAndDeletedAtIsNull_Works() {
        // Given
        Profile profile = new Profile();
        profile.setUserId(200L);
        profile.setTenantId(TENANT_1);
        profile.setFullName("Test User");
        profile.setHeadline("Tester");
        entityManager.persist(profile);

        entityManager.flush();
        entityManager.clear();

        // When/Then
        boolean exists = profileRepository.existsByUserIdAndDeletedAtIsNull(200L);
        assertThat(exists).isTrue();

        boolean notExists = profileRepository.existsByUserIdAndDeletedAtIsNull(999L);
        assertThat(notExists).isFalse();
    }

    @Test
    void findByUserIdAndDeletedAtIsNull_Works() {
        // Given
        Profile profile = new Profile();
        profile.setUserId(300L);
        profile.setTenantId(TENANT_1);
        profile.setFullName("Find User");
        profile.setHeadline("Developer");
        entityManager.persist(profile);

        entityManager.flush();
        entityManager.clear();

        // When
        Optional<Profile> result = profileRepository.findByUserIdAndDeletedAtIsNull(300L);

        // Then
        assertThat(result).isPresent();
        assertThat(result.get().getFullName()).isEqualTo("Find User");
    }
}
