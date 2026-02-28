package com.interviewme.integration;

import com.interviewme.common.util.TenantContext;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import com.interviewme.service.AccountDeletionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AccountDeletionIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private JobExperienceRepository jobExperienceRepository;
    @Autowired private EducationRepository educationRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private AccountDeletionService accountDeletionService;
    @Autowired private AccountDeletionRepository accountDeletionRepository;

    private Long tenantId;
    private Long userId;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("deletion-tenant-" + System.nanoTime());
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail("deletion-" + System.nanoTime() + "@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);
        userId = user.getId();

        TenantContext.setTenantId(tenantId);
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    @Test
    @Transactional
    void deleteAccount_removesAllData() {
        // Create a full account with profile, jobs, education, skills
        Profile profile = new Profile();
        profile.setTenantId(tenantId);
        profile.setUserId(userId);
        profile.setFullName("To Delete");
        profile.setHeadline("Dev");
        profile = profileRepository.save(profile);
        Long profileId = profile.getId();

        JobExperience job = new JobExperience();
        job.setTenantId(tenantId);
        job.setProfileId(profileId);
        job.setCompany("DeleteCo");
        job.setRole("Dev");
        job.setStartDate(LocalDate.of(2020, 1, 1));
        jobExperienceRepository.save(job);

        Education edu = new Education();
        edu.setTenantId(tenantId);
        edu.setProfileId(profileId);
        edu.setDegree("BS CS");
        edu.setInstitution("Test U");
        edu.setEndDate(LocalDate.of(2019, 6, 1));
        educationRepository.save(edu);

        Skill skill = skillRepository.findByNameIgnoreCase("Python").orElseGet(() -> {
            Skill s = new Skill();
            s.setName("Python");
            s.setCategory("Programming");
            return skillRepository.save(s);
        });

        UserSkill us = new UserSkill();
        us.setTenantId(tenantId);
        us.setProfileId(profileId);
        us.setSkillId(skill.getId());
        us.setYearsOfExperience(3);
        us.setProficiencyDepth(7);
        userSkillRepository.save(us);

        // Verify data exists
        assertThat(profileRepository.findByIdAndTenantId(profileId, tenantId)).isPresent();
        assertThat(jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId)).hasSize(1);
        assertThat(educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId)).hasSize(1);
        assertThat(userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(profileId)).hasSize(1);

        // Delete the entire account
        Map<String, Integer> counts = accountDeletionService.deleteAccount(tenantId, userId);

        // Verify counts
        assertThat(counts.get("profiles")).isEqualTo(1);
        assertThat(counts.get("users")).isEqualTo(1);
        assertThat(counts.get("jobExperiences")).isEqualTo(1);
        assertThat(counts.get("educations")).isEqualTo(1);
        assertThat(counts.get("userSkills")).isEqualTo(1);
        assertThat(counts.get("tenants")).isEqualTo(1);

        // Verify everything is gone
        assertThat(profileRepository.findByIdAndTenantId(profileId, tenantId)).isEmpty();
        assertThat(userRepository.findById(userId)).isEmpty();
        assertThat(tenantRepository.findById(tenantId)).isEmpty();
    }

    @Test
    @Transactional
    void deleteAccount_handlesEmptyAccount() {
        // Account with just user and tenant, no profile or data
        Map<String, Integer> counts = accountDeletionService.deleteAccount(tenantId, userId);

        assertThat(counts.get("users")).isEqualTo(1);
        assertThat(counts.get("tenants")).isEqualTo(1);
        assertThat(counts.get("profiles")).isEqualTo(0);
        assertThat(counts.get("jobExperiences")).isEqualTo(0);
    }
}
