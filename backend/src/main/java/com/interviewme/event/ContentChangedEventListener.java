package com.interviewme.event;

import com.interviewme.aichat.model.ContentType;
import com.interviewme.aichat.service.EmbeddingService;
import com.interviewme.model.Education;
import com.interviewme.model.ExperienceProject;
import com.interviewme.model.Profile;
import com.interviewme.model.Story;
import com.interviewme.model.JobExperience;
import com.interviewme.model.UserSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ContentChangedEventListener {

    private final EmbeddingService embeddingService;

    public void onSkillChanged(UserSkill skill) {
        if ("public".equals(skill.getVisibility()) && skill.getDeletedAt() == null) {
            String text = formatSkillText(skill);
            embeddingService.generateEmbedding(skill.getTenantId(), ContentType.SKILL, skill.getId(), text);
        } else {
            embeddingService.deleteEmbedding(skill.getTenantId(), ContentType.SKILL, skill.getId());
        }
    }

    public void onStoryChanged(Story story) {
        if ("public".equals(story.getVisibility()) && story.getDeletedAt() == null) {
            String text = formatStoryText(story);
            embeddingService.generateEmbedding(story.getTenantId(), ContentType.STORY, story.getId(), text);
        } else {
            embeddingService.deleteEmbedding(story.getTenantId(), ContentType.STORY, story.getId());
        }
    }

    public void onProjectChanged(ExperienceProject project) {
        if ("public".equals(project.getVisibility()) && project.getDeletedAt() == null) {
            String text = formatProjectText(project);
            embeddingService.generateEmbedding(project.getTenantId(), ContentType.PROJECT, project.getId(), text);
        } else {
            embeddingService.deleteEmbedding(project.getTenantId(), ContentType.PROJECT, project.getId());
        }
    }

    public void onJobChanged(JobExperience job) {
        if ("public".equals(job.getVisibility()) && job.getDeletedAt() == null) {
            String text = formatJobText(job);
            embeddingService.generateEmbedding(job.getTenantId(), ContentType.JOB, job.getId(), text);
        } else {
            embeddingService.deleteEmbedding(job.getTenantId(), ContentType.JOB, job.getId());
        }
    }

    public void onEducationChanged(Education education) {
        if ("public".equals(education.getVisibility()) && education.getDeletedAt() == null) {
            String text = formatEducationText(education);
            embeddingService.generateEmbedding(education.getTenantId(), ContentType.EDUCATION, education.getId(), text);
        } else {
            embeddingService.deleteEmbedding(education.getTenantId(), ContentType.EDUCATION, education.getId());
        }
    }

    public void onProfileChanged(Profile profile) {
        if (profile.getDeletedAt() == null && profile.getSummary() != null && !profile.getSummary().isBlank()) {
            String text = formatProfileSummaryText(profile);
            embeddingService.generateEmbedding(profile.getTenantId(), ContentType.PROFILE_SUMMARY, profile.getId(), text);
        } else {
            embeddingService.deleteEmbedding(profile.getTenantId(), ContentType.PROFILE_SUMMARY, profile.getId());
        }
    }

    public static String formatSkillText(UserSkill skill) {
        String skillName = skill.getSkill() != null ? skill.getSkill().getName() : "Unknown";
        String category = skill.getSkill() != null ? skill.getSkill().getCategory() : "General";
        String confidence = skill.getConfidenceLevel() != null ? skill.getConfidenceLevel() : "MEDIUM";
        return "Skill: %s - Category: %s - %d years of experience - Proficiency: %d/5 - Confidence: %s".formatted(
                skillName, category, skill.getYearsOfExperience(), skill.getProficiencyDepth(), confidence);
    }

    public static String formatStoryText(Story story) {
        return "%s. Situation: %s. Task: %s. Action: %s. Result: %s".formatted(
                story.getTitle(),
                story.getSituation(),
                story.getTask(),
                story.getAction(),
                story.getResult());
    }

    public static String formatProjectText(ExperienceProject project) {
        String techStack = project.getTechStack() != null ? String.join(", ", project.getTechStack()) : "";
        StringBuilder sb = new StringBuilder();
        sb.append("Project: ").append(project.getTitle()).append(". ");
        if (project.getContext() != null && !project.getContext().isBlank()) {
            sb.append(project.getContext()).append(" ");
        }
        if (project.getRole() != null && !project.getRole().isBlank()) {
            sb.append("Role: ").append(project.getRole()).append(". ");
        }
        if (project.getTeamSize() != null && project.getTeamSize() > 0) {
            sb.append("Team size: ").append(project.getTeamSize()).append(". ");
        }
        if (!techStack.isBlank()) {
            sb.append("Technologies: ").append(techStack).append(". ");
        }
        if (project.getArchitectureType() != null && !project.getArchitectureType().isBlank()) {
            sb.append("Architecture: ").append(project.getArchitectureType()).append(". ");
        }
        if (project.getOutcomes() != null && !project.getOutcomes().isBlank()) {
            sb.append("Outcomes: ").append(project.getOutcomes());
        }
        return sb.toString().trim();
    }

    public static String formatJobText(JobExperience job) {
        String endDate = job.getEndDate() != null ? job.getEndDate().toString() : "present";
        StringBuilder sb = new StringBuilder();
        sb.append("Work Experience: ").append(job.getCompany());
        sb.append(" - ").append(job.getRole());
        sb.append(" (").append(job.getStartDate().toString()).append(" to ").append(endDate).append(")");
        if (job.getLocation() != null && !job.getLocation().isBlank()) {
            sb.append(". Location: ").append(job.getLocation());
        }
        if (job.getEmploymentType() != null && !job.getEmploymentType().isBlank()) {
            sb.append(". Type: ").append(job.getEmploymentType());
        }
        if (job.getResponsibilities() != null && !job.getResponsibilities().isBlank()) {
            sb.append(". Responsibilities: ").append(job.getResponsibilities());
        }
        if (job.getAchievements() != null && !job.getAchievements().isBlank()) {
            sb.append(". Key achievements: ").append(job.getAchievements());
        }
        return sb.toString();
    }

    public static String formatEducationText(Education education) {
        StringBuilder sb = new StringBuilder();
        sb.append("Education: ").append(education.getInstitution());
        sb.append(" - ").append(education.getDegree());
        if (education.getFieldOfStudy() != null && !education.getFieldOfStudy().isBlank()) {
            sb.append(" in ").append(education.getFieldOfStudy());
        }
        if (education.getStartDate() != null) {
            sb.append(" (").append(education.getStartDate());
            sb.append(" to ").append(education.getEndDate()).append(")");
        }
        if (education.getNotes() != null && !education.getNotes().isBlank()) {
            sb.append(". ").append(education.getNotes());
        }
        return sb.toString();
    }

    public static String formatProfileSummaryText(Profile profile) {
        StringBuilder sb = new StringBuilder();
        sb.append("Professional Summary for ").append(profile.getFullName()).append(": ");
        sb.append(profile.getHeadline()).append(". ");
        sb.append(profile.getSummary());
        if (profile.getLocation() != null && !profile.getLocation().isBlank()) {
            sb.append(" Location: ").append(profile.getLocation()).append(".");
        }
        List<String> languages = profile.getLanguages();
        if (languages != null && !languages.isEmpty()) {
            sb.append(" Languages: ").append(String.join(", ", languages)).append(".");
        }
        return sb.toString();
    }
}
