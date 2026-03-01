package com.interviewme.integration;

import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.profile.CreateProfileRequest;
import com.interviewme.dto.profile.ProfileResponse;
import com.interviewme.linkedin.LinkedInImportMappingService;
import com.interviewme.linkedin.dto.LinkedInImportData;
import com.interviewme.linkedin.service.LinkedInZipParserService;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import com.interviewme.service.ProfileService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;

class LinkedInImportIntegrationTest extends AbstractIntegrationTest {

    @Autowired private TenantRepository tenantRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private ProfileService profileService;
    @Autowired private ProfileRepository profileRepository;
    @Autowired private JobExperienceRepository jobExperienceRepository;
    @Autowired private EducationRepository educationRepository;
    @Autowired private SkillRepository skillRepository;
    @Autowired private UserSkillRepository userSkillRepository;
    @Autowired private LinkedInImportRepository linkedInImportRepository;
    @Autowired private LinkedInZipParserService zipParserService;
    @Autowired private LinkedInImportMappingService mappingService;
    @Autowired private EntityManager entityManager;

    private Long tenantId;
    private Long userId;
    private Long profileId;

    @BeforeEach
    void setUp() {
        Tenant tenant = new Tenant();
        tenant.setName("import-tenant-" + System.nanoTime());
        tenant = tenantRepository.save(tenant);
        tenantId = tenant.getId();

        User user = new User();
        user.setTenantId(tenantId);
        user.setEmail("import-" + System.nanoTime() + "@test.com");
        user.setPasswordHash("hash");
        user = userRepository.save(user);
        userId = user.getId();

        TenantContext.setTenantId(tenantId);

        // Create a profile for the user
        CreateProfileRequest profileReq = new CreateProfileRequest(
                "Test User", "Test Headline", "Test summary", "Test Location", null, null, null, null
        );
        ProfileResponse profileResp = profileService.createProfile(userId, profileReq);
        profileId = profileResp.id();
    }

    @AfterEach
    void tearDown() {
        TenantContext.clear();
    }

    // ==================== ZIP PARSER TESTS ====================

    @Test
    void parseZip_withValidLinkedInExport_parsesAllCsvFiles() throws IOException {
        byte[] zipBytes = createTestLinkedInZip();
        LinkedInImportData data = zipParserService.parseZip(new ByteArrayInputStream(zipBytes));

        assertThat(data).isNotNull();

        // Profile
        assertThat(data.profile()).isNotNull();
        assertThat(data.profile().firstName()).isEqualTo("Felipe");
        assertThat(data.profile().lastName()).isEqualTo("Gomes");
        assertThat(data.profile().headline()).isEqualTo("Senior Software Engineer");
        assertThat(data.profile().summary()).contains("10+ years");

        // Positions
        assertThat(data.positions()).hasSize(3);
        assertThat(data.positions().get(0).companyName()).isEqualTo("TechCorp");
        assertThat(data.positions().get(0).title()).isEqualTo("Senior Software Engineer");
        assertThat(data.positions().get(0).startDate()).isEqualTo("Jan 2022");
        assertThat(data.positions().get(0).endDate()).isNull(); // Current position

        // Education
        assertThat(data.education()).hasSize(2);
        assertThat(data.education().get(0).schoolName()).isEqualTo("University of Lisbon");
        assertThat(data.education().get(0).degreeName()).isEqualTo("Bachelor of Computer Science");

        // Skills
        assertThat(data.skills()).hasSize(10);
        assertThat(data.skills()).contains("Java", "Spring Boot", "Docker", "PostgreSQL");

        // Languages
        assertThat(data.languages()).hasSize(3);
        assertThat(data.languages()).contains("Portuguese", "English", "German");
    }

    @Test
    void parseZip_withMissingCsvFiles_returnsWarnings() throws IOException {
        // Create a ZIP with only Profile.csv
        byte[] zipBytes = createZipWithFiles(Map.of(
                "Profile.csv", readTestResource("linkedin-test-data/Profile.csv")
        ));

        LinkedInImportData data = zipParserService.parseZip(new ByteArrayInputStream(zipBytes));

        assertThat(data.profile()).isNotNull();
        assertThat(data.positions()).isEmpty();
        assertThat(data.education()).isEmpty();
        assertThat(data.skills()).isEmpty();
        assertThat(data.warnings()).isNotEmpty();
        assertThat(data.warnings()).anyMatch(w -> w.contains("Positions.csv not found"));
        assertThat(data.warnings()).anyMatch(w -> w.contains("Skills.csv not found"));
    }

    @Test
    void parseZip_withEmptyZip_returnsEmptyDataWithWarnings() throws IOException {
        byte[] zipBytes = createZipWithFiles(Map.of());

        LinkedInImportData data = zipParserService.parseZip(new ByteArrayInputStream(zipBytes));

        assertThat(data.profile()).isNull();
        assertThat(data.positions()).isEmpty();
        assertThat(data.skills()).isEmpty();
        assertThat(data.warnings()).isNotEmpty();
    }

    // ==================== MERGE IMPORT TESTS ====================

    @Test
    @Transactional
    void executeImport_mergeStrategy_importsAllData() throws IOException {
        byte[] zipBytes = createTestLinkedInZip();
        LinkedInImportData data = zipParserService.parseZip(new ByteArrayInputStream(zipBytes));

        LinkedInImport result = mappingService.executeImport(
                tenantId, profileId, data, ImportStrategy.MERGE, "test-export.zip");

        assertThat(result).isNotNull();
        assertThat(result.getStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(result.getImportedAt()).isNotNull();

        // Verify item counts
        Map<String, Integer> counts = result.getItemCounts();
        assertThat(counts.get("profile")).isEqualTo(1);
        assertThat(counts.get("jobs")).isEqualTo(3);
        assertThat(counts.get("education")).isEqualTo(2);
        assertThat(counts.get("skills")).isEqualTo(10);
        assertThat(counts.get("languages")).isEqualTo(3);

        // Verify profile was updated
        Profile updatedProfile = profileRepository.findByIdAndTenantId(profileId, tenantId).orElseThrow();
        assertThat(updatedProfile.getFullName()).isEqualTo("Felipe Gomes");
        assertThat(updatedProfile.getHeadline()).isEqualTo("Senior Software Engineer");
        assertThat(updatedProfile.getSummary()).contains("10+ years");

        // Verify jobs were created
        var jobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);
        assertThat(jobs).hasSize(3);
        assertThat(jobs).extracting(JobExperience::getCompany)
                .containsExactlyInAnyOrder("TechCorp", "CloudStart", "DataFlow Inc");

        // Verify current job has no end date
        var currentJob = jobs.stream().filter(j -> j.getCompany().equals("TechCorp")).findFirst().orElseThrow();
        assertThat(currentJob.getIsCurrent()).isTrue();
        assertThat(currentJob.getEndDate()).isNull();

        // Verify education
        var education = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId);
        assertThat(education).hasSize(2);
        assertThat(education).extracting(Education::getInstitution)
                .containsExactlyInAnyOrder("University of Lisbon", "MIT Online");

        // Verify skills were created in catalog and linked to user
        var userSkills = userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(profileId);
        assertThat(userSkills).hasSize(10);

        // Verify languages
        Profile profileWithLangs = profileRepository.findByIdAndTenantId(profileId, tenantId).orElseThrow();
        assertThat(profileWithLangs.getLanguages()).containsExactlyInAnyOrder("Portuguese", "English", "German");
    }

    @Test
    @Transactional
    void executeImport_mergeStrategy_skipsDuplicates() throws IOException {
        // First: create some existing data
        JobExperience existingJob = new JobExperience();
        existingJob.setTenantId(tenantId);
        existingJob.setProfileId(profileId);
        existingJob.setCompany("TechCorp");
        existingJob.setRole("Senior Software Engineer");
        existingJob.setStartDate(LocalDate.of(2022, 1, 1));
        existingJob.setVisibility("public");
        jobExperienceRepository.save(existingJob);

        Education existingEdu = new Education();
        existingEdu.setTenantId(tenantId);
        existingEdu.setProfileId(profileId);
        existingEdu.setInstitution("University of Lisbon");
        existingEdu.setDegree("Bachelor of Computer Science");
        existingEdu.setEndDate(LocalDate.of(2016, 6, 1));
        existingEdu.setVisibility("public");
        educationRepository.save(existingEdu);

        // Import with MERGE
        byte[] zipBytes = createTestLinkedInZip();
        LinkedInImportData data = zipParserService.parseZip(new ByteArrayInputStream(zipBytes));
        LinkedInImport result = mappingService.executeImport(
                tenantId, profileId, data, ImportStrategy.MERGE, "test-export.zip");

        assertThat(result.getStatus()).isEqualTo(ImportStatus.COMPLETED);

        // TechCorp and University of Lisbon should be skipped (duplicates)
        assertThat(result.getItemCounts().get("jobs")).isEqualTo(2); // only CloudStart and DataFlow
        assertThat(result.getItemCounts().get("education")).isEqualTo(1); // only MIT Online

        // Verify total counts in DB (existing + new)
        var allJobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);
        assertThat(allJobs).hasSize(3); // 1 existing + 2 new

        var allEdu = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId);
        assertThat(allEdu).hasSize(2); // 1 existing + 1 new
    }

    // ==================== OVERWRITE IMPORT TESTS ====================

    @Test
    @Transactional
    void executeImport_overwriteStrategy_replacesExistingData() throws IOException {
        // First: create existing data
        JobExperience existingJob = new JobExperience();
        existingJob.setTenantId(tenantId);
        existingJob.setProfileId(profileId);
        existingJob.setCompany("OldCompany");
        existingJob.setRole("Old Role");
        existingJob.setStartDate(LocalDate.of(2010, 1, 1));
        existingJob.setVisibility("public");
        jobExperienceRepository.save(existingJob);

        Skill oldSkill = new Skill();
        oldSkill.setName("OldSkill-" + System.nanoTime());
        oldSkill.setCategory("Legacy");
        oldSkill.setIsActive(true);
        oldSkill = skillRepository.save(oldSkill);

        UserSkill oldUserSkill = new UserSkill();
        oldUserSkill.setTenantId(tenantId);
        oldUserSkill.setProfileId(profileId);
        oldUserSkill.setSkillId(oldSkill.getId());
        oldUserSkill.setProficiencyDepth(3);
        oldUserSkill.setVisibility("public");
        userSkillRepository.save(oldUserSkill);

        // Verify existing data is there
        assertThat(jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId)).hasSize(1);
        assertThat(userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(profileId)).hasSize(1);

        // Import with OVERWRITE
        byte[] zipBytes = createTestLinkedInZip();
        LinkedInImportData data = zipParserService.parseZip(new ByteArrayInputStream(zipBytes));
        LinkedInImport result = mappingService.executeImport(
                tenantId, profileId, data, ImportStrategy.OVERWRITE, "test-export.zip");

        assertThat(result.getStatus()).isEqualTo(ImportStatus.COMPLETED);

        // Old data should be soft-deleted, only new data remains
        var activeJobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);
        assertThat(activeJobs).hasSize(3);
        assertThat(activeJobs).extracting(JobExperience::getCompany)
                .doesNotContain("OldCompany");

        var activeSkills = userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(profileId);
        assertThat(activeSkills).hasSize(10);
    }

    // ==================== IMPORT HISTORY TESTS ====================

    @Test
    @Transactional
    void executeImport_createsImportHistoryRecord() throws IOException {
        byte[] zipBytes = createTestLinkedInZip();
        LinkedInImportData data = zipParserService.parseZip(new ByteArrayInputStream(zipBytes));

        LinkedInImport result = mappingService.executeImport(
                tenantId, profileId, data, ImportStrategy.MERGE, "my-linkedin-export.zip");

        // Verify import record
        assertThat(result.getId()).isNotNull();
        assertThat(result.getTenantId()).isEqualTo(tenantId);
        assertThat(result.getProfileId()).isEqualTo(profileId);
        assertThat(result.getFilename()).isEqualTo("my-linkedin-export.zip");
        assertThat(result.getImportStrategy()).isEqualTo(ImportStrategy.MERGE);
        assertThat(result.getStatus()).isEqualTo(ImportStatus.COMPLETED);
        assertThat(result.getItemCounts()).isNotEmpty();
        assertThat(result.getCreatedAt()).isNotNull();

        // Verify history query
        entityManager.flush();
        List<LinkedInImport> history = linkedInImportRepository
                .findByTenantIdAndProfileIdOrderByCreatedAtDesc(tenantId, profileId);
        assertThat(history).hasSize(1);
        assertThat(history.get(0).getFilename()).isEqualTo("my-linkedin-export.zip");
    }

    // ==================== DATE PARSING TESTS ====================

    @Test
    @Transactional
    void executeImport_parsesLinkedInDateFormats() throws IOException {
        byte[] zipBytes = createTestLinkedInZip();
        LinkedInImportData data = zipParserService.parseZip(new ByteArrayInputStream(zipBytes));

        mappingService.executeImport(tenantId, profileId, data, ImportStrategy.MERGE, "test.zip");

        var jobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);

        // "Jan 2022" should parse to 2022-01-01
        var techCorpJob = jobs.stream().filter(j -> "TechCorp".equals(j.getCompany())).findFirst().orElseThrow();
        assertThat(techCorpJob.getStartDate()).isEqualTo(LocalDate.of(2022, 1, 1));

        // "Mar 2019" should parse to 2019-03-01
        var cloudJob = jobs.stream().filter(j -> "CloudStart".equals(j.getCompany())).findFirst().orElseThrow();
        assertThat(cloudJob.getStartDate()).isEqualTo(LocalDate.of(2019, 3, 1));
        assertThat(cloudJob.getEndDate()).isEqualTo(LocalDate.of(2021, 12, 1));

        // Education with year-only dates: "2012" -> 2012-01-01
        var education = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId);
        var lisbon = education.stream().filter(e -> "University of Lisbon".equals(e.getInstitution())).findFirst().orElseThrow();
        assertThat(lisbon.getStartDate()).isEqualTo(LocalDate.of(2012, 1, 1));
    }

    // ==================== TENANT ISOLATION TEST ====================

    @Test
    @Transactional
    void executeImport_respectsTenantIsolation() throws IOException {
        // Import data for tenant 1
        byte[] zipBytes = createTestLinkedInZip();
        LinkedInImportData data = zipParserService.parseZip(new ByteArrayInputStream(zipBytes));
        mappingService.executeImport(tenantId, profileId, data, ImportStrategy.MERGE, "test.zip");

        // Create tenant 2
        Tenant tenant2 = new Tenant();
        tenant2.setName("other-tenant-" + System.nanoTime());
        tenant2 = tenantRepository.save(tenant2);
        Long tenantId2 = tenant2.getId();

        User user2 = new User();
        user2.setTenantId(tenantId2);
        user2.setEmail("other-" + System.nanoTime() + "@test.com");
        user2.setPasswordHash("hash");
        user2 = userRepository.save(user2);

        TenantContext.setTenantId(tenantId2);

        CreateProfileRequest profileReq2 = new CreateProfileRequest(
                "Other User", "Other Headline", "Other summary", "Other Location", null, null, null, null
        );
        ProfileResponse profileResp2 = profileService.createProfile(user2.getId(), profileReq2);

        // Tenant 2 should have no imported jobs
        var tenant2Jobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileResp2.id());
        assertThat(tenant2Jobs).isEmpty();

        // Tenant 2 should have no import history
        var tenant2History = linkedInImportRepository.findByTenantIdAndProfileIdOrderByCreatedAtDesc(tenantId2, profileResp2.id());
        assertThat(tenant2History).isEmpty();

        // Switch back to verify tenant 1 still has data
        TenantContext.setTenantId(tenantId);
        var tenant1Jobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);
        assertThat(tenant1Jobs).hasSize(3);
    }

    // ==================== HELPER METHODS ====================

    private byte[] createTestLinkedInZip() throws IOException {
        return createZipWithFiles(Map.of(
                "Profile.csv", readTestResource("linkedin-test-data/Profile.csv"),
                "Positions.csv", readTestResource("linkedin-test-data/Positions.csv"),
                "Education.csv", readTestResource("linkedin-test-data/Education.csv"),
                "Skills.csv", readTestResource("linkedin-test-data/Skills.csv"),
                "Languages.csv", readTestResource("linkedin-test-data/Languages.csv")
        ));
    }

    private byte[] createZipWithFiles(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, byte[]> entry : files.entrySet()) {
                zos.putNextEntry(new ZipEntry(entry.getKey()));
                zos.write(entry.getValue());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }

    private byte[] readTestResource(String path) throws IOException {
        ClassPathResource resource = new ClassPathResource(path);
        try (InputStream is = resource.getInputStream()) {
            return is.readAllBytes();
        }
    }
}
