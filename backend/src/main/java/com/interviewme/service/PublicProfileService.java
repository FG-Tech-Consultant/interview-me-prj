package com.interviewme.service;

import com.interviewme.model.ExperienceProject;
import com.interviewme.model.ExperienceProjectSkill;
import com.interviewme.model.Story;
import com.interviewme.model.StorySkill;
import com.interviewme.repository.ExperienceProjectRepository;
import com.interviewme.repository.ExperienceProjectSkillRepository;
import com.interviewme.repository.StoryRepository;
import com.interviewme.repository.StorySkillRepository;
import com.interviewme.model.Education;
import com.interviewme.model.JobExperience;
import com.interviewme.model.Profile;
import com.interviewme.repository.EducationRepository;
import com.interviewme.repository.JobExperienceRepository;
import com.interviewme.repository.ProfileRepository;
import com.interviewme.dto.publicprofile.*;
import com.interviewme.common.exception.PublicProfileNotFoundException;
import com.interviewme.util.SlugValidator;
import com.interviewme.model.UserSkill;
import com.interviewme.repository.UserSkillRepository;
import com.interviewme.billing.config.BillingProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PublicProfileService {

    private final ProfileRepository profileRepository;
    private final JobExperienceRepository jobExperienceRepository;
    private final EducationRepository educationRepository;
    private final UserSkillRepository userSkillRepository;
    private final ExperienceProjectRepository experienceProjectRepository;
    private final StoryRepository storyRepository;
    private final ExperienceProjectSkillRepository experienceProjectSkillRepository;
    private final StorySkillRepository storySkillRepository;
    private final BillingProperties billingProperties;

    @Transactional
    public PublicProfileResponse getPublicProfile(String slug) {
        log.info("Fetching public profile for slug: {}", slug);

        Profile profile = profileRepository.findBySlugAndDeletedAtIsNull(slug)
                .orElseThrow(() -> new PublicProfileNotFoundException(slug));

        Long profileId = profile.getId();

        // Fetch public jobs (visibility = 'public', deleted_at IS NULL)
        List<JobExperience> publicJobs = jobExperienceRepository
                .findByProfileIdAndVisibilityAndDeletedAtIsNull(profileId, "public");

        // Fetch public education
        List<Education> publicEducation = educationRepository
                .findByProfileIdAndVisibilityAndDeletedAtIsNull(profileId, "public");

        // Fetch public skills with catalog join
        List<UserSkill> publicSkills = userSkillRepository
                .findByProfileIdAndVisibilityAndDeletedAtIsNull(profileId, "public");

        // Fetch public projects under public jobs
        List<Long> publicJobIds = publicJobs.stream()
                .map(JobExperience::getId)
                .toList();

        List<ExperienceProject> publicProjects = publicJobIds.isEmpty()
                ? Collections.emptyList()
                : experienceProjectRepository
                    .findByJobExperienceIdInAndVisibilityAndDeletedAtIsNull(publicJobIds, "public");

        // Fetch public stories under public projects
        List<Long> publicProjectIds = publicProjects.stream()
                .map(ExperienceProject::getId)
                .toList();

        List<Story> publicStories = publicProjectIds.isEmpty()
                ? Collections.emptyList()
                : storyRepository
                    .findByExperienceProjectIdInAndVisibilityAndDeletedAtIsNull(publicProjectIds, "public");

        // Build skill name lookup from UserSkill for linked skills in projects/stories
        Map<Long, String> userSkillNameMap = buildUserSkillNameMap(publicSkills, profileId);

        // Build project skill links
        Map<Long, List<String>> projectSkillNames = buildProjectSkillNames(publicProjectIds, userSkillNameMap);

        // Build story skill links
        Map<Long, List<String>> storySkillNames = buildStorySkillNames(publicProjectIds, publicStories, userSkillNameMap);

        // Assemble nested response
        PublicProfileResponse response = assemblePublicProfile(
                profile, publicJobs, publicEducation, publicSkills,
                publicProjects, publicStories, projectSkillNames, storySkillNames);

        // Fire-and-forget view count increment
        incrementViewCount(profile.getId());

        return response;
    }

    @Transactional(readOnly = true)
    public SlugCheckResponse checkSlugAvailability(String slug) {
        String normalized = SlugValidator.normalizeSlug(slug);
        int changeCost = billingProperties.getCosts().getOrDefault("SLUG_CHANGE", 5);

        if (!SlugValidator.isValidSlug(normalized)) {
            return new SlugCheckResponse(normalized, false, Collections.emptyList(), changeCost);
        }

        if (SlugValidator.isReservedSlug(normalized)) {
            return new SlugCheckResponse(normalized, false, Collections.emptyList(), changeCost);
        }

        boolean taken = profileRepository.existsBySlugGlobally(normalized);
        if (taken) {
            List<String> suggestions = generateSuggestions(normalized);
            return new SlugCheckResponse(normalized, false, suggestions, changeCost);
        }

        return new SlugCheckResponse(normalized, true, Collections.emptyList(), changeCost);
    }

    public void incrementViewCount(Long profileId) {
        try {
            profileRepository.incrementViewCount(profileId);
        } catch (Exception ex) {
            log.warn("Failed to increment view count for profile {}: {}", profileId, ex.getMessage());
        }
    }

    private Map<Long, String> buildUserSkillNameMap(List<UserSkill> publicSkills, Long profileId) {
        // Build map from userSkill ID -> skill name
        Map<Long, String> map = new HashMap<>();
        for (UserSkill us : publicSkills) {
            if (us.getSkill() != null) {
                map.put(us.getId(), us.getSkill().getName());
            }
        }
        return map;
    }

    private Map<Long, List<String>> buildProjectSkillNames(List<Long> projectIds, Map<Long, String> userSkillNameMap) {
        if (projectIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<String>> result = new HashMap<>();
        for (Long projectId : projectIds) {
            List<ExperienceProjectSkill> links = experienceProjectSkillRepository.findByExperienceProjectId(projectId);
            List<String> names = links.stream()
                    .map(link -> userSkillNameMap.get(link.getUserSkillId()))
                    .filter(Objects::nonNull)
                    .toList();
            result.put(projectId, names);
        }
        return result;
    }

    private Map<Long, List<String>> buildStorySkillNames(List<Long> projectIds, List<Story> stories, Map<Long, String> userSkillNameMap) {
        if (stories.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, List<String>> result = new HashMap<>();
        for (Story story : stories) {
            List<StorySkill> links = storySkillRepository.findByStoryId(story.getId());
            List<String> names = links.stream()
                    .map(link -> userSkillNameMap.get(link.getUserSkillId()))
                    .filter(Objects::nonNull)
                    .toList();
            result.put(story.getId(), names);
        }
        return result;
    }

    private PublicProfileResponse assemblePublicProfile(
            Profile profile,
            List<JobExperience> publicJobs,
            List<Education> publicEducation,
            List<UserSkill> publicSkills,
            List<ExperienceProject> publicProjects,
            List<Story> publicStories,
            Map<Long, List<String>> projectSkillNames,
            Map<Long, List<String>> storySkillNames) {

        // Map stories by project ID
        Map<Long, List<Story>> storiesByProject = publicStories.stream()
                .collect(Collectors.groupingBy(Story::getExperienceProjectId));

        // Map projects by job ID
        Map<Long, List<ExperienceProject>> projectsByJob = publicProjects.stream()
                .collect(Collectors.groupingBy(ExperienceProject::getJobExperienceId));

        // Build skill responses sorted by proficiency desc
        List<PublicSkillResponse> skillResponses = publicSkills.stream()
                .sorted(Comparator.comparingInt(UserSkill::getProficiencyDepth).reversed())
                .map(us -> new PublicSkillResponse(
                        us.getSkill() != null ? us.getSkill().getName() : "Unknown",
                        us.getSkill() != null ? us.getSkill().getCategory() : "Other",
                        us.getProficiencyDepth(),
                        us.getYearsOfExperience(),
                        us.getLastUsedDate()
                ))
                .toList();

        // Build job responses with nested projects and stories
        List<PublicJobResponse> jobResponses = publicJobs.stream()
                .sorted(Comparator.comparing(JobExperience::getStartDate).reversed())
                .map(job -> {
                    List<ExperienceProject> jobProjects = projectsByJob.getOrDefault(job.getId(), Collections.emptyList());
                    List<PublicProjectResponse> projectResponses = jobProjects.stream()
                            .map(proj -> {
                                List<Story> projStories = storiesByProject.getOrDefault(proj.getId(), Collections.emptyList());
                                List<PublicStoryResponse> storyResponses = projStories.stream()
                                        .map(story -> new PublicStoryResponse(
                                                story.getTitle(),
                                                story.getSituation(),
                                                story.getTask(),
                                                story.getAction(),
                                                story.getResult(),
                                                story.getMetrics(),
                                                storySkillNames.getOrDefault(story.getId(), Collections.emptyList())
                                        ))
                                        .toList();
                                return new PublicProjectResponse(
                                        proj.getTitle(),
                                        proj.getContext(),
                                        proj.getRole(),
                                        proj.getTeamSize(),
                                        proj.getTechStack(),
                                        proj.getArchitectureType(),
                                        proj.getMetrics(),
                                        proj.getOutcomes(),
                                        projectSkillNames.getOrDefault(proj.getId(), Collections.emptyList()),
                                        storyResponses
                                );
                            })
                            .toList();
                    return new PublicJobResponse(
                            job.getCompany(),
                            job.getRole(),
                            job.getStartDate(),
                            job.getEndDate(),
                            Boolean.TRUE.equals(job.getIsCurrent()),
                            job.getLocation(),
                            job.getEmploymentType(),
                            job.getResponsibilities(),
                            job.getAchievements(),
                            job.getMetrics(),
                            job.getWorkLanguage(),
                            projectResponses
                    );
                })
                .toList();

        // Build education responses
        List<PublicEducationResponse> educationResponses = publicEducation.stream()
                .sorted(Comparator.comparing(Education::getEndDate).reversed())
                .map(edu -> new PublicEducationResponse(
                        edu.getDegree(),
                        edu.getInstitution(),
                        edu.getStartDate(),
                        edu.getEndDate(),
                        edu.getFieldOfStudy()
                ))
                .toList();

        // Build SEO metadata
        SeoMetadata seo = buildSeoMetadata(profile, skillResponses);

        return new PublicProfileResponse(
                profile.getSlug(),
                profile.getFullName(),
                profile.getHeadline(),
                profile.getSummary(),
                profile.getLocation(),
                profile.getLanguages(),
                profile.getProfessionalLinks(),
                skillResponses,
                jobResponses,
                educationResponses,
                seo
        );
    }

    private SeoMetadata buildSeoMetadata(Profile profile, List<PublicSkillResponse> skills) {
        String title = profile.getHeadline() != null
                ? profile.getFullName() + " - " + profile.getHeadline() + " | Interview Me"
                : profile.getFullName() + " | Interview Me";
        String description = profile.getSummary() != null
                ? profile.getSummary().substring(0, Math.min(profile.getSummary().length(), 160))
                : (profile.getHeadline() != null ? profile.getHeadline() : profile.getFullName());
        String canonicalUrl = "https://interviewme.app/p/" + profile.getSlug();
        List<String> keywords = skills.stream()
                .map(PublicSkillResponse::skillName)
                .limit(10)
                .toList();

        return new SeoMetadata(title, description, canonicalUrl, keywords);
    }

    private List<String> generateSuggestions(String slug) {
        List<String> suggestions = new ArrayList<>();
        for (int i = 1; i <= 3; i++) {
            String candidate = slug + "-" + i;
            if (!profileRepository.existsBySlugGlobally(candidate) && SlugValidator.isValidSlug(candidate)) {
                suggestions.add(candidate);
            }
        }
        return suggestions;
    }
}
