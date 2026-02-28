package com.interviewme.integration;

import com.interviewme.common.util.TenantContext;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class TenantIsolationIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private JobExperienceRepository jobExperienceRepository;
    @Autowired private EducationRepository educationRepository;

    private Long tenantId1;
    private Long tenantId2;
    private Long profileId1;
    private Long profileId2;

    @BeforeEach
    void setUp() {
        // Create two tenants
        Tenant t1 = new Tenant();
        t1.setName("tenant-iso-1-" + System.nanoTime());
        t1 = tenantRepository.save(t1);
        tenantId1 = t1.getId();

        Tenant t2 = new Tenant();
        t2.setName("tenant-iso-2-" + System.nanoTime());
        t2 = tenantRepository.save(t2);
        tenantId2 = t2.getId();

        // Create users
        User u1 = new User();
        u1.setTenantId(tenantId1);
        u1.setEmail("iso-user1-" + System.nanoTime() + "@test.com");
        u1.setPasswordHash("hash");
        u1 = userRepository.save(u1);

        User u2 = new User();
        u2.setTenantId(tenantId2);
        u2.setEmail("iso-user2-" + System.nanoTime() + "@test.com");
        u2.setPasswordHash("hash");
        u2 = userRepository.save(u2);

        // Create profiles
        Profile p1 = new Profile();
        p1.setTenantId(tenantId1);
        p1.setUserId(u1.getId());
        p1.setFullName("Tenant 1 User");
        p1.setHeadline("Developer");
        p1 = profileRepository.save(p1);
        profileId1 = p1.getId();

        Profile p2 = new Profile();
        p2.setTenantId(tenantId2);
        p2.setUserId(u2.getId());
        p2.setFullName("Tenant 2 User");
        p2.setHeadline("Engineer");
        p2 = profileRepository.save(p2);
        profileId2 = p2.getId();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @Transactional
    void profileRepository_findByIdAndTenantId_isolatesTenants() {
        // Tenant 1 can access their own profile
        Optional<Profile> own = profileRepository.findByIdAndTenantId(profileId1, tenantId1);
        assertThat(own).isPresent();
        assertThat(own.get().getFullName()).isEqualTo("Tenant 1 User");

        // Tenant 1 cannot access tenant 2's profile
        Optional<Profile> cross = profileRepository.findByIdAndTenantId(profileId2, tenantId1);
        assertThat(cross).isEmpty();
    }

    @Test
    @Transactional
    void jobExperienceRepository_isolatesTenants() {
        // Create job for tenant 1
        JobExperience job1 = new JobExperience();
        job1.setTenantId(tenantId1);
        job1.setProfileId(profileId1);
        job1.setCompany("Company A");
        job1.setRole("Senior Dev");
        job1.setStartDate(LocalDate.of(2020, 1, 1));
        job1 = jobExperienceRepository.save(job1);

        // Create job for tenant 2
        JobExperience job2 = new JobExperience();
        job2.setTenantId(tenantId2);
        job2.setProfileId(profileId2);
        job2.setCompany("Company B");
        job2.setRole("Junior Dev");
        job2.setStartDate(LocalDate.of(2021, 6, 1));
        jobExperienceRepository.save(job2);

        // Tenant 1 profile should only return tenant 1 jobs
        var tenant1Jobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId1);
        assertThat(tenant1Jobs).hasSize(1);
        assertThat(tenant1Jobs.get(0).getCompany()).isEqualTo("Company A");

        // Cross-tenant profile query returns nothing (job belongs to profileId1, not profileId2)
        Optional<JobExperience> crossTenant = jobExperienceRepository.findByIdAndProfileIdAndDeletedAtIsNull(
            job1.getId(), profileId2);
        assertThat(crossTenant).isEmpty();
    }

    @Test
    @Transactional
    void educationRepository_isolatesTenants() {
        Education edu1 = new Education();
        edu1.setTenantId(tenantId1);
        edu1.setProfileId(profileId1);
        edu1.setDegree("BS Computer Science");
        edu1.setInstitution("MIT");
        edu1.setEndDate(LocalDate.of(2020, 5, 15));
        educationRepository.save(edu1);

        Education edu2 = new Education();
        edu2.setTenantId(tenantId2);
        edu2.setProfileId(profileId2);
        edu2.setDegree("MS Engineering");
        edu2.setInstitution("Stanford");
        edu2.setEndDate(LocalDate.of(2022, 6, 15));
        educationRepository.save(edu2);

        var tenant1Edu = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId1);
        assertThat(tenant1Edu).hasSize(1);
        assertThat(tenant1Edu.get(0).getDegree()).isEqualTo("BS Computer Science");

        var tenant2Edu = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId2);
        assertThat(tenant2Edu).hasSize(1);
        assertThat(tenant2Edu.get(0).getDegree()).isEqualTo("MS Engineering");
    }
}
