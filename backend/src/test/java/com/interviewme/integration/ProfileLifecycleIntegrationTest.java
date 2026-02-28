package com.interviewme.integration;

import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.profile.CreateProfileRequest;
import com.interviewme.dto.profile.ProfileResponse;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import com.interviewme.service.ProfileService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileLifecycleIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileService profileService;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private JobExperienceRepository jobExperienceRepository;
    @Autowired private EducationRepository educationRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private UserSkillRepository userSkillRepository;

    private Long tenantId;
    private Long userId;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("lifecycle-tenant-" + System.nanoTime());
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail("lifecycle-" + System.nanoTime() + "@test.com");
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
    void fullProfileLifecycle_createProfileWithAssociations() {
        // 1. Create profile
        CreateProfileRequest request = new CreateProfileRequest(
            "John Doe", "Senior Developer", "Experienced dev", "San Francisco",
            List.of("English", "Spanish"), null, null, null
        );
        ProfileResponse profile = profileService.createProfile(userId, request);
        assertThat(profile).isNotNull();
        assertThat(profile.fullName()).isEqualTo("John Doe");
        assertThat(profile.slug()).isNotNull(); // Auto-generated slug

        Long profileId = profile.id();

        // 2. Add job experiences
        JobExperience job1 = new JobExperience();
        job1.setTenantId(tenantId);
        job1.setProfileId(profileId);
        job1.setCompany("Google");
        job1.setRole("Software Engineer");
        job1.setStartDate(LocalDate.of(2018, 1, 1));
        job1.setEndDate(LocalDate.of(2022, 12, 31));
        job1.setVisibility("public");
        jobExperienceRepository.save(job1);

        JobExperience job2 = new JobExperience();
        job2.setTenantId(tenantId);
        job2.setProfileId(profileId);
        job2.setCompany("Meta");
        job2.setRole("Senior Engineer");
        job2.setStartDate(LocalDate.of(2023, 1, 1));
        job2.setIsCurrent(true);
        job2.setVisibility("public");
        jobExperienceRepository.save(job2);

        // 3. Add education
        Education edu = new Education();
        edu.setTenantId(tenantId);
        edu.setProfileId(profileId);
        edu.setDegree("BS Computer Science");
        edu.setInstitution("Stanford University");
        edu.setEndDate(LocalDate.of(2017, 6, 15));
        edu.setVisibility("public");
        educationRepository.save(edu);

        // 4. Add skill
        Skill javaSkill = skillRepository.findByNameIgnoreCase("Java").orElseGet(() -> {
            Skill s = new Skill();
            s.setName("Java");
            s.setCategory("Programming");
            return skillRepository.save(s);
        });

        UserSkill userSkill = new UserSkill();
        userSkill.setTenantId(tenantId);
        userSkill.setProfileId(profileId);
        userSkill.setSkillId(javaSkill.getId());
        userSkill.setYearsOfExperience(5);
        userSkill.setProficiencyDepth(8);
        userSkill.setVisibility("public");
        userSkillRepository.save(userSkill);

        // 5. Verify full profile assembly
        var jobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);
        assertThat(jobs).hasSize(2);

        var educations = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId);
        assertThat(educations).hasSize(1);

        var skills = userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(profileId);
        assertThat(skills).hasSize(1);
        assertThat(skills.get(0).getSkill().getName()).isEqualTo("Java");

        // 6. Verify profile retrieval
        ProfileResponse retrieved = profileService.getProfileByUserId(userId);
        assertThat(retrieved.fullName()).isEqualTo("John Doe");
        assertThat(retrieved.headline()).isEqualTo("Senior Developer");
    }

    @Test
    @Transactional
    void profileSlug_autoGeneratedAndUnique() {
        CreateProfileRequest request1 = new CreateProfileRequest(
            "Alice Smith", "Developer", null, null, null, null, null, null
        );
        ProfileResponse p1 = profileService.createProfile(userId, request1);
        assertThat(p1.slug()).isNotNull();
        assertThat(p1.slug()).isNotBlank();

        // Create another user for a second profile
        User user2 = new User();
        user2.setTenantId(tenantId);
        user2.setEmail("lifecycle2-" + System.nanoTime() + "@test.com");
        user2.setPasswordHash("hash");
        user2 = userRepository.save(user2);

        CreateProfileRequest request2 = new CreateProfileRequest(
            "Alice Smith", "Engineer", null, null, null, null, null, null
        );
        ProfileResponse p2 = profileService.createProfile(user2.getId(), request2);
        assertThat(p2.slug()).isNotNull();

        // Slugs should be different even for same name
        assertThat(p1.slug()).isNotEqualTo(p2.slug());
    }
}
