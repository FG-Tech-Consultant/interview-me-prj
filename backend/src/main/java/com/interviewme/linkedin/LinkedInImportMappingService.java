package com.interviewme.linkedin;

import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.linkedin.dto.*;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

@Service
@Slf4j
@RequiredArgsConstructor
public class LinkedInImportMappingService {

    private static final DateTimeFormatter MONTH_YEAR_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    private final ProfileRepository profileRepository;
    private final JobExperienceRepository jobExperienceRepository;
    private final EducationRepository educationRepository;
    private final SkillRepository skillRepository;
    private final UserSkillRepository userSkillRepository;
    private final LinkedInImportRepository linkedInImportRepository;
    private final ContentChangedEventListener contentChangedEventListener;

    @Transactional
    public LinkedInImport executeImport(Long tenantId, Long profileId, LinkedInImportData data, ImportStrategy strategy, String filename) {
        List<String> errors = new ArrayList<>();
        Map<String, Integer> itemCounts = new LinkedHashMap<>();

        LinkedInImport importRecord = LinkedInImport.builder()
                .tenantId(tenantId)
                .profileId(profileId)
                .status(ImportStatus.PENDING)
                .filename(filename)
                .importStrategy(strategy)
                .build();
        importRecord = linkedInImportRepository.save(importRecord);

        try {
            if (strategy == ImportStrategy.OVERWRITE) {
                deleteExistingData(profileId);
            }

            int profileCount = importProfile(tenantId, profileId, data.profile(), errors);
            itemCounts.put("profile", profileCount);

            int jobCount = importJobs(tenantId, profileId, data.positions(), strategy, errors);
            itemCounts.put("jobs", jobCount);

            int eduCount = importEducation(tenantId, profileId, data.education(), strategy, errors);
            itemCounts.put("education", eduCount);

            int skillCount = importSkills(tenantId, profileId, data.skills(), strategy, errors);
            itemCounts.put("skills", skillCount);

            int langCount = importLanguages(profileId, data.languages(), strategy, errors);
            itemCounts.put("languages", langCount);

            importRecord.setStatus(ImportStatus.COMPLETED);
            importRecord.setImportedAt(Instant.now());

            // Generate RAG embeddings for all imported content
            generateEmbeddings(tenantId, profileId);
        } catch (Exception e) {
            log.error("Import failed for tenant={}, profile={}", tenantId, profileId, e);
            importRecord.setStatus(ImportStatus.FAILED);
            errors.add("Import failed: " + e.getMessage());
        }

        importRecord.setItemCounts(itemCounts);
        importRecord.setErrors(errors.isEmpty() ? null : errors);
        return linkedInImportRepository.save(importRecord);
    }

    private void deleteExistingData(Long profileId) {
        List<JobExperience> jobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);
        jobs.forEach(j -> {
            j.setDeletedAt(Instant.now());
            jobExperienceRepository.save(j);
        });

        List<Education> edus = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId);
        edus.forEach(e -> {
            e.setDeletedAt(Instant.now());
            educationRepository.save(e);
        });

        List<UserSkill> skills = userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(profileId);
        skills.forEach(s -> {
            s.setDeletedAt(Instant.now());
            userSkillRepository.save(s);
        });
    }

    private int importProfile(Long tenantId, Long profileId, LinkedInProfileData profileData, List<String> errors) {
        if (profileData == null) return 0;

        try {
            Optional<Profile> optProfile = profileRepository.findByIdAndTenantId(profileId, tenantId);
            if (optProfile.isEmpty()) {
                errors.add("Profile not found for id=" + profileId);
                return 0;
            }

            Profile profile = optProfile.get();
            if (profileData.firstName() != null || profileData.lastName() != null) {
                String fullName = ((profileData.firstName() != null ? profileData.firstName() : "") + " " +
                        (profileData.lastName() != null ? profileData.lastName() : "")).trim();
                if (!fullName.isBlank()) {
                    profile.setFullName(fullName);
                }
            }
            if (profileData.headline() != null) {
                profile.setHeadline(profileData.headline());
            }
            if (profileData.summary() != null) {
                profile.setSummary(profileData.summary());
            }
            if (profileData.location() != null) {
                profile.setLocation(profileData.location());
            }

            profileRepository.save(profile);
            return 1;
        } catch (Exception e) {
            errors.add("Failed to update profile: " + e.getMessage());
            return 0;
        }
    }

    private int importJobs(Long tenantId, Long profileId, List<LinkedInPositionData> positions, ImportStrategy strategy, List<String> errors) {
        if (positions == null || positions.isEmpty()) return 0;

        List<JobExperience> existingJobs = strategy == ImportStrategy.MERGE
                ? jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId)
                : List.of();

        int count = 0;
        for (LinkedInPositionData pos : positions) {
            try {
                if (strategy == ImportStrategy.MERGE && isDuplicateJob(existingJobs, pos)) {
                    log.debug("Skipping duplicate job: {} at {}", pos.title(), pos.companyName());
                    continue;
                }

                LocalDate startDate = parseLinkedInDate(pos.startDate());
                if (startDate == null) {
                    startDate = LocalDate.of(2000, 1, 1); // fallback
                }
                LocalDate endDate = parseLinkedInDate(pos.endDate());

                JobExperience job = new JobExperience();
                job.setTenantId(tenantId);
                job.setProfileId(profileId);
                job.setCompany(pos.companyName() != null ? pos.companyName() : "Unknown");
                job.setRole(pos.title() != null ? pos.title() : "Unknown");
                job.setResponsibilities(pos.description());
                job.setLocation(pos.location());
                job.setStartDate(startDate);
                job.setEndDate(endDate);
                job.setIsCurrent(endDate == null);
                job.setVisibility("public");

                jobExperienceRepository.save(job);
                count++;
            } catch (Exception e) {
                errors.add("Failed to import job '" + pos.title() + "' at '" + pos.companyName() + "': " + e.getMessage());
            }
        }
        return count;
    }

    private boolean isDuplicateJob(List<JobExperience> existing, LinkedInPositionData pos) {
        return existing.stream().anyMatch(j ->
                equalsIgnoreCaseNullable(j.getCompany(), pos.companyName()) &&
                equalsIgnoreCaseNullable(j.getRole(), pos.title()));
    }

    private int importEducation(Long tenantId, Long profileId, List<LinkedInEducationData> educationList, ImportStrategy strategy, List<String> errors) {
        if (educationList == null || educationList.isEmpty()) return 0;

        List<Education> existingEdu = strategy == ImportStrategy.MERGE
                ? educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId)
                : List.of();

        int count = 0;
        for (LinkedInEducationData edu : educationList) {
            try {
                if (strategy == ImportStrategy.MERGE && isDuplicateEducation(existingEdu, edu)) {
                    log.debug("Skipping duplicate education: {} at {}", edu.degreeName(), edu.schoolName());
                    continue;
                }

                LocalDate startDate = parseLinkedInDate(edu.startDate());
                LocalDate endDate = parseLinkedInDate(edu.endDate());
                if (endDate == null) {
                    endDate = LocalDate.now(); // fallback for end date (required field)
                }

                Education education = new Education();
                education.setTenantId(tenantId);
                education.setProfileId(profileId);
                education.setInstitution(edu.schoolName() != null ? edu.schoolName() : "Unknown");
                education.setDegree(edu.degreeName() != null ? edu.degreeName() : "Unknown");
                education.setFieldOfStudy(edu.fieldOfStudy());
                education.setStartDate(startDate);
                education.setEndDate(endDate);
                education.setVisibility("public");

                // Combine notes and activities
                String notes = combineNotesAndActivities(edu.notes(), edu.activities());
                education.setNotes(notes);

                educationRepository.save(education);
                count++;
            } catch (Exception e) {
                errors.add("Failed to import education '" + edu.degreeName() + "' at '" + edu.schoolName() + "': " + e.getMessage());
            }
        }
        return count;
    }

    private boolean isDuplicateEducation(List<Education> existing, LinkedInEducationData edu) {
        return existing.stream().anyMatch(e ->
                equalsIgnoreCaseNullable(e.getInstitution(), edu.schoolName()) &&
                equalsIgnoreCaseNullable(e.getDegree(), edu.degreeName()));
    }

    private int importSkills(Long tenantId, Long profileId, List<String> skillNames, ImportStrategy strategy, List<String> errors) {
        if (skillNames == null || skillNames.isEmpty()) return 0;

        int count = 0;
        for (String skillName : skillNames) {
            try {
                // Find or create the skill in the catalog
                Skill skill = skillRepository.findByNameIgnoreCase(skillName)
                        .orElseGet(() -> {
                            Skill newSkill = new Skill();
                            newSkill.setName(skillName);
                            newSkill.setCategory("Imported");
                            newSkill.setIsActive(true);
                            return skillRepository.save(newSkill);
                        });

                // Check for existing UserSkill (MERGE mode)
                if (strategy == ImportStrategy.MERGE) {
                    Optional<UserSkill> existing = userSkillRepository.findByProfileIdAndSkillIdAndDeletedAtIsNull(profileId, skill.getId());
                    if (existing.isPresent()) {
                        log.debug("Skipping duplicate skill: {}", skillName);
                        continue;
                    }
                }

                UserSkill userSkill = new UserSkill();
                userSkill.setTenantId(tenantId);
                userSkill.setProfileId(profileId);
                userSkill.setSkillId(skill.getId());
                userSkill.setProficiencyDepth(5);
                userSkill.setVisibility("public");

                userSkillRepository.save(userSkill);
                count++;
            } catch (Exception e) {
                errors.add("Failed to import skill '" + skillName + "': " + e.getMessage());
            }
        }
        return count;
    }

    private int importLanguages(Long profileId, List<String> languages, ImportStrategy strategy, List<String> errors) {
        if (languages == null || languages.isEmpty()) return 0;

        try {
            Optional<Profile> optProfile = profileRepository.findByIdAndDeletedAtIsNull(profileId);
            if (optProfile.isEmpty()) {
                errors.add("Profile not found for language import");
                return 0;
            }

            Profile profile = optProfile.get();

            if (strategy == ImportStrategy.OVERWRITE) {
                profile.setLanguages(new ArrayList<>(languages));
                profileRepository.save(profile);
                return languages.size();
            }

            // MERGE: deduplicate by language name (part before " - ")
            List<String> existingLangs = profile.getLanguages() != null ? new ArrayList<>(profile.getLanguages()) : new ArrayList<>();

            int added = 0;
            for (String lang : languages) {
                String langName = extractLanguageName(lang);
                boolean exists = existingLangs.stream()
                        .anyMatch(l -> extractLanguageName(l).equalsIgnoreCase(langName));
                if (!exists) {
                    existingLangs.add(lang);
                    added++;
                }
            }

            profile.setLanguages(existingLangs);
            profileRepository.save(profile);
            return added;
        } catch (Exception e) {
            errors.add("Failed to import languages: " + e.getMessage());
            return 0;
        }
    }

    private String extractLanguageName(String language) {
        int dashIndex = language.indexOf(" - ");
        return dashIndex > 0 ? language.substring(0, dashIndex).trim() : language.trim();
    }

    private LocalDate parseLinkedInDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) return null;

        dateStr = dateStr.trim();

        // Try "MMM yyyy" (e.g., "Jan 2020")
        try {
            return LocalDate.parse("01 " + dateStr, DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
        } catch (DateTimeParseException ignored) {}

        // Try "yyyy" (e.g., "2020") -> January 1st
        try {
            int year = Integer.parseInt(dateStr);
            return LocalDate.of(year, 1, 1);
        } catch (NumberFormatException ignored) {}

        log.warn("Unable to parse LinkedIn date: '{}'", dateStr);
        return null;
    }

    private String combineNotesAndActivities(String notes, String activities) {
        StringBuilder sb = new StringBuilder();
        if (notes != null && !notes.isBlank()) {
            sb.append(notes.trim());
        }
        if (activities != null && !activities.isBlank()) {
            if (!sb.isEmpty()) sb.append("\n\n");
            sb.append("Activities: ").append(activities.trim());
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    private boolean equalsIgnoreCaseNullable(String a, String b) {
        if (a == null && b == null) return true;
        if (a == null || b == null) return false;
        return a.equalsIgnoreCase(b);
    }

    private void generateEmbeddings(Long tenantId, Long profileId) {
        try {
            // Profile summary + languages
            profileRepository.findByIdAndTenantId(profileId, tenantId)
                    .ifPresent(profile -> {
                        try { contentChangedEventListener.onProfileChanged(profile); }
                        catch (Exception e) { log.warn("Failed to generate profile embedding: {}", e.getMessage()); }
                    });

            // Jobs
            List<JobExperience> jobs = jobExperienceRepository.findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);
            for (JobExperience job : jobs) {
                try { contentChangedEventListener.onJobChanged(job); }
                catch (Exception e) { log.warn("Failed to generate job embedding for {}: {}", job.getId(), e.getMessage()); }
            }

            // Education
            List<Education> edus = educationRepository.findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId);
            for (Education edu : edus) {
                try { contentChangedEventListener.onEducationChanged(edu); }
                catch (Exception e) { log.warn("Failed to generate education embedding for {}: {}", edu.getId(), e.getMessage()); }
            }

            // Skills (reload to get Skill entity via eager fetch)
            List<UserSkill> skills = userSkillRepository.findByProfileIdAndDeletedAtIsNullOrderBySkill_CategoryAscSkill_NameAsc(profileId);
            for (UserSkill skill : skills) {
                try { contentChangedEventListener.onSkillChanged(skill); }
                catch (Exception e) { log.warn("Failed to generate skill embedding for {}: {}", skill.getId(), e.getMessage()); }
            }

            log.info("Generated embeddings for LinkedIn import: profile={}, jobs={}, education={}, skills={}",
                    profileId, jobs.size(), edus.size(), skills.size());
        } catch (Exception e) {
            log.warn("Failed to generate embeddings after LinkedIn import for profile {}: {}", profileId, e.getMessage());
        }
    }
}
