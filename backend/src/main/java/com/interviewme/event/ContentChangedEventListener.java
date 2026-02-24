package com.interviewme.event;

import com.interviewme.aichat.model.ContentType;
import com.interviewme.aichat.service.EmbeddingService;
import com.interviewme.model.ExperienceProject;
import com.interviewme.model.Story;
import com.interviewme.model.JobExperience;
import com.interviewme.model.UserSkill;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

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

    public static String formatSkillText(UserSkill skill) {
        String skillName = skill.getSkill() != null ? skill.getSkill().getName() : "Unknown";
        String category = skill.getSkill() != null ? skill.getSkill().getCategory() : "General";
        return "%s - Category: %s - %d years experience - Proficiency: %d/5".formatted(
                skillName, category, skill.getYearsOfExperience(), skill.getProficiencyDepth());
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
        return "%s. %s. Technologies: %s. Outcomes: %s".formatted(
                project.getTitle(),
                project.getContext() != null ? project.getContext() : "",
                techStack,
                project.getOutcomes() != null ? project.getOutcomes() : "");
    }

    public static String formatJobText(JobExperience job) {
        String endDate = job.getEndDate() != null ? job.getEndDate().toString() : "present";
        return "%s - %s (%s to %s). Key achievements: %s".formatted(
                job.getCompany(),
                job.getRole(),
                job.getStartDate().toString(),
                endDate,
                job.getAchievements() != null ? job.getAchievements() : "");
    }
}
