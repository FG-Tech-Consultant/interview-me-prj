package com.interviewme.service;

import com.interviewme.linkedin.model.ProfileSection;
import com.interviewme.model.*;
import com.interviewme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.format.DateTimeFormatter;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Collects profile data from the system's registered profile and formats it
 * into sections compatible with the LinkedIn analysis prompt.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileDataCollectorService {

    private final ProfileRepository profileRepository;
    private final JobExperienceRepository jobExperienceRepository;
    private final EducationRepository educationRepository;
    private final UserSkillRepository userSkillRepository;
    private final ExperienceProjectRepository experienceProjectRepository;
    private final StoryRepository storyRepository;

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy");

    @Transactional(readOnly = true)
    public Map<ProfileSection, String> collectProfileData(Long profileId, Long tenantId) {
        log.info("Collecting profile data for profileId={}, tenantId={}", profileId, tenantId);

        Map<ProfileSection, String> sections = new EnumMap<>(ProfileSection.class);

        // Initialize all sections with empty strings
        for (ProfileSection section : ProfileSection.values()) {
            sections.put(section, "");
        }

        Profile profile = profileRepository.findByIdAndTenantId(profileId, tenantId).orElse(null);
        if (profile == null) {
            log.warn("Profile not found for profileId={}, tenantId={}", profileId, tenantId);
            return sections;
        }

        // HEADLINE
        if (profile.getHeadline() != null && !profile.getHeadline().isBlank()) {
            sections.put(ProfileSection.HEADLINE, profile.getHeadline());
        }

        // ABOUT
        if (profile.getSummary() != null && !profile.getSummary().isBlank()) {
            sections.put(ProfileSection.ABOUT, profile.getSummary());
        }

        // EXPERIENCE (jobs + projects + stories)
        String experienceText = buildExperienceSection(profileId);
        if (!experienceText.isBlank()) {
            sections.put(ProfileSection.EXPERIENCE, experienceText);
        }

        // EDUCATION
        String educationText = buildEducationSection(profileId);
        if (!educationText.isBlank()) {
            sections.put(ProfileSection.EDUCATION, educationText);
        }

        // SKILLS
        String skillsText = buildSkillsSection(profileId, tenantId);
        if (!skillsText.isBlank()) {
            sections.put(ProfileSection.SKILLS, skillsText);
        }

        // RECOMMENDATIONS - not stored in system, leave empty

        // OTHER - languages, location
        String otherText = buildOtherSection(profile);
        if (!otherText.isBlank()) {
            sections.put(ProfileSection.OTHER, otherText);
        }

        log.info("Profile data collected. Sections with content: {}",
                sections.entrySet().stream()
                        .filter(e -> !e.getValue().isBlank())
                        .map(e -> e.getKey().name())
                        .toList());

        return sections;
    }

    private String buildExperienceSection(Long profileId) {
        List<JobExperience> jobs = jobExperienceRepository
                .findByProfileIdAndDeletedAtIsNullOrderByStartDateDesc(profileId);

        if (jobs.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (JobExperience job : jobs) {
            sb.append(job.getRole()).append(" at ").append(job.getCompany());

            String dateRange = formatDateRange(job);
            if (!dateRange.isEmpty()) {
                sb.append(" (").append(dateRange).append(")");
            }
            sb.append("\n");

            if (job.getLocation() != null && !job.getLocation().isBlank()) {
                sb.append("Location: ").append(job.getLocation()).append("\n");
            }

            if (job.getResponsibilities() != null && !job.getResponsibilities().isBlank()) {
                sb.append("Responsibilities: ").append(job.getResponsibilities()).append("\n");
            }

            if (job.getAchievements() != null && !job.getAchievements().isBlank()) {
                sb.append("Achievements: ").append(job.getAchievements()).append("\n");
            }

            // Include projects for this job
            List<ExperienceProject> projects = experienceProjectRepository
                    .findByJobExperienceIdAndDeletedAtIsNullOrderByCreatedAtDesc(job.getId());

            for (ExperienceProject project : projects) {
                sb.append("  Project: ").append(project.getTitle()).append("\n");
                if (project.getContext() != null && !project.getContext().isBlank()) {
                    sb.append("    Context: ").append(project.getContext()).append("\n");
                }
                if (project.getOutcomes() != null && !project.getOutcomes().isBlank()) {
                    sb.append("    Outcomes: ").append(project.getOutcomes()).append("\n");
                }
                if (project.getTechStack() != null && !project.getTechStack().isEmpty()) {
                    sb.append("    Tech: ").append(String.join(", ", project.getTechStack())).append("\n");
                }

                // Include stories (STAR format) for this project
                List<Story> stories = storyRepository
                        .findByExperienceProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(project.getId());

                for (Story story : stories) {
                    sb.append("    Story: ").append(story.getTitle()).append("\n");
                    sb.append("      Situation: ").append(story.getSituation()).append("\n");
                    sb.append("      Task: ").append(story.getTask()).append("\n");
                    sb.append("      Action: ").append(story.getAction()).append("\n");
                    sb.append("      Result: ").append(story.getResult()).append("\n");
                }
            }

            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private String buildEducationSection(Long profileId) {
        List<Education> educationList = educationRepository
                .findByProfileIdAndDeletedAtIsNullOrderByEndDateDesc(profileId);

        if (educationList.isEmpty()) {
            return "";
        }

        StringBuilder sb = new StringBuilder();
        for (Education edu : educationList) {
            sb.append(edu.getDegree());
            if (edu.getFieldOfStudy() != null && !edu.getFieldOfStudy().isBlank()) {
                sb.append(" in ").append(edu.getFieldOfStudy());
            }
            sb.append(" - ").append(edu.getInstitution());

            if (edu.getStartDate() != null && edu.getEndDate() != null) {
                sb.append(" (")
                        .append(edu.getStartDate().format(DATE_FORMAT))
                        .append(" - ")
                        .append(edu.getEndDate().format(DATE_FORMAT))
                        .append(")");
            } else if (edu.getEndDate() != null) {
                sb.append(" (").append(edu.getEndDate().format(DATE_FORMAT)).append(")");
            }
            sb.append("\n");

            if (edu.getGpa() != null && !edu.getGpa().isBlank()) {
                sb.append("GPA: ").append(edu.getGpa()).append("\n");
            }

            if (edu.getNotes() != null && !edu.getNotes().isBlank()) {
                sb.append("Notes: ").append(edu.getNotes()).append("\n");
            }

            sb.append("\n");
        }

        return sb.toString().trim();
    }

    private String buildSkillsSection(Long profileId, Long tenantId) {
        List<UserSkill> userSkills = userSkillRepository.findByProfileIdAndTenantId(profileId, tenantId);

        if (userSkills.isEmpty()) {
            return "";
        }

        return userSkills.stream()
                .filter(us -> us.getSkill() != null)
                .map(us -> {
                    StringBuilder skillEntry = new StringBuilder(us.getSkill().getName());
                    if (us.getSkill().getCategory() != null && !us.getSkill().getCategory().isBlank()) {
                        skillEntry.append(" (").append(us.getSkill().getCategory()).append(")");
                    }
                    if (us.getYearsOfExperience() != null && us.getYearsOfExperience() > 0) {
                        skillEntry.append(" - ").append(us.getYearsOfExperience()).append(" years");
                    }
                    return skillEntry.toString();
                })
                .collect(Collectors.joining("\n"));
    }

    private String buildOtherSection(Profile profile) {
        StringBuilder sb = new StringBuilder();

        if (profile.getLocation() != null && !profile.getLocation().isBlank()) {
            sb.append("Location: ").append(profile.getLocation()).append("\n");
        }

        if (profile.getLanguages() != null && !profile.getLanguages().isEmpty()) {
            sb.append("Languages: ").append(String.join(", ", profile.getLanguages())).append("\n");
        }

        if (profile.getProfessionalLinks() != null && !profile.getProfessionalLinks().isEmpty()) {
            sb.append("Professional Links:\n");
            profile.getProfessionalLinks().forEach((key, value) ->
                    sb.append("  ").append(key).append(": ").append(value).append("\n"));
        }

        return sb.toString().trim();
    }

    private String formatDateRange(JobExperience job) {
        StringBuilder range = new StringBuilder();
        if (job.getStartDate() != null) {
            range.append(job.getStartDate().format(DATE_FORMAT));
        }
        if (Boolean.TRUE.equals(job.getIsCurrent())) {
            range.append(" - Present");
        } else if (job.getEndDate() != null) {
            range.append(" - ").append(job.getEndDate().format(DATE_FORMAT));
        }
        return range.toString();
    }
}
