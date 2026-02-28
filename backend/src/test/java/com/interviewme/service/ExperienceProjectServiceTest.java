package com.interviewme.service;

import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.ProjectNotFoundException;
import com.interviewme.common.exception.ValidationException;
import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.experience.CreateProjectRequest;
import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.UpdateProjectRequest;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.model.ExperienceProject;
import com.interviewme.model.JobExperience;
import com.interviewme.model.Story;
import com.interviewme.repository.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ExperienceProjectService")
class ExperienceProjectServiceTest {

    @Mock private ExperienceProjectRepository projectRepository;
    @Mock private ExperienceProjectSkillRepository projectSkillRepository;
    @Mock private StoryRepository storyRepository;
    @Mock private StorySkillRepository storySkillRepository;
    @Mock private JobExperienceRepository jobExperienceRepository;
    @Mock private ContentChangedEventListener contentChangedEventListener;

    @InjectMocks
    private ExperienceProjectService experienceProjectService;

    private MockedStatic<TenantContext> tenantContextMock;

    private static final Long TENANT_ID = 100L;
    private static final Long JOB_ID = 5L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getCurrentTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    private ExperienceProject buildProject(Long id) {
        ExperienceProject p = new ExperienceProject();
        p.setId(id);
        p.setTenantId(TENANT_ID);
        p.setJobExperienceId(JOB_ID);
        p.setTitle("Auth Service");
        p.setContext("Built OAuth2 service");
        p.setRole("Tech Lead");
        p.setTeamSize(5);
        p.setVisibility("private");
        return p;
    }

    @Nested
    @DisplayName("createProject")
    class CreateProject {

        @Test
        @DisplayName("creates project when job experience exists")
        void createsProjectSuccessfully() {
            JobExperience job = new JobExperience();
            job.setId(JOB_ID);
            when(jobExperienceRepository.findByIdAndDeletedAtIsNull(JOB_ID)).thenReturn(Optional.of(job));

            ExperienceProject saved = buildProject(10L);
            when(projectRepository.save(any(ExperienceProject.class))).thenReturn(saved);

            CreateProjectRequest request = new CreateProjectRequest(
                    "Auth Service", "OAuth2", "Tech Lead", 5,
                    List.of("Java"), "Microservice", null, "Reduced auth time", "private");

            ProjectResponse result = experienceProjectService.createProject(JOB_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("Auth Service");
            verify(projectRepository).save(argThat(p -> p.getTenantId().equals(TENANT_ID)));
        }

        @Test
        @DisplayName("throws ValidationException when job experience missing")
        void throwsWhenJobMissing() {
            when(jobExperienceRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            CreateProjectRequest request = new CreateProjectRequest(
                    "Title", null, null, null, null, null, null, null, null);

            assertThatThrownBy(() -> experienceProjectService.createProject(99L, request))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("updateProject")
    class UpdateProject {

        @Test
        @DisplayName("updates project successfully with story count")
        void updatesProjectSuccessfully() {
            ExperienceProject existing = buildProject(10L);
            when(projectRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(existing));
            when(projectRepository.save(any(ExperienceProject.class))).thenReturn(existing);
            when(storyRepository.countByExperienceProjectIdAndDeletedAtIsNull(10L)).thenReturn(3);

            UpdateProjectRequest request = new UpdateProjectRequest(
                    "Updated Auth", "Updated context", "Senior Lead", 8,
                    List.of("Java", "Spring"), "Monolith", null, "Better", "public", 1L);

            ProjectResponse result = experienceProjectService.updateProject(10L, request);

            assertThat(result).isNotNull();
            assertThat(result.storyCount()).isEqualTo(3);
        }

        @Test
        @DisplayName("throws ProjectNotFoundException when project missing")
        void throwsWhenMissing() {
            when(projectRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> experienceProjectService.updateProject(99L,
                    new UpdateProjectRequest("T", null, null, null, null, null, null, null, null, 1L)))
                    .isInstanceOf(ProjectNotFoundException.class);
        }

        @Test
        @DisplayName("throws OptimisticLockException on concurrent modification")
        void throwsOnOptimisticLock() {
            ExperienceProject existing = buildProject(10L);
            when(projectRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(existing));
            when(projectRepository.save(any(ExperienceProject.class)))
                    .thenThrow(new OptimisticLockingFailureException("conflict"));

            assertThatThrownBy(() -> experienceProjectService.updateProject(10L,
                    new UpdateProjectRequest("T", null, null, null, null, null, null, null, null, 1L)))
                    .isInstanceOf(OptimisticLockException.class);
        }
    }

    @Nested
    @DisplayName("deleteProject")
    class DeleteProject {

        @Test
        @DisplayName("soft deletes project and cascades to stories")
        void softDeletesCascading() {
            ExperienceProject project = buildProject(10L);
            when(projectRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));

            Story s1 = new Story();
            s1.setId(20L);
            s1.setExperienceProjectId(10L);
            Story s2 = new Story();
            s2.setId(21L);
            s2.setExperienceProjectId(10L);

            when(storyRepository.findByExperienceProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(10L))
                    .thenReturn(List.of(s1, s2));
            when(storyRepository.save(any(Story.class))).thenAnswer(inv -> inv.getArgument(0));
            when(projectRepository.save(any(ExperienceProject.class))).thenReturn(project);

            experienceProjectService.deleteProject(10L);

            // Verify cascade: both stories soft-deleted
            assertThat(s1.getDeletedAt()).isNotNull();
            assertThat(s2.getDeletedAt()).isNotNull();
            assertThat(project.getDeletedAt()).isNotNull();

            // Verify skill associations cleaned up
            verify(storySkillRepository).deleteByStoryId(20L);
            verify(storySkillRepository).deleteByStoryId(21L);
            verify(projectSkillRepository).deleteByExperienceProjectId(10L);
        }

        @Test
        @DisplayName("throws ProjectNotFoundException when project missing")
        void throwsWhenMissing() {
            when(projectRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> experienceProjectService.deleteProject(99L))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getProjectsByJobExperience")
    class GetProjectsByJobExperience {

        @Test
        @DisplayName("returns projects with story counts")
        void returnsProjectsWithStoryCounts() {
            JobExperience job = new JobExperience();
            job.setId(JOB_ID);
            when(jobExperienceRepository.findByIdAndDeletedAtIsNull(JOB_ID)).thenReturn(Optional.of(job));

            ExperienceProject p1 = buildProject(10L);
            ExperienceProject p2 = buildProject(11L);
            when(projectRepository.findByJobExperienceIdAndDeletedAtIsNullOrderByCreatedAtDesc(JOB_ID))
                    .thenReturn(List.of(p1, p2));
            when(storyRepository.countByExperienceProjectIdAndDeletedAtIsNull(10L)).thenReturn(2);
            when(storyRepository.countByExperienceProjectIdAndDeletedAtIsNull(11L)).thenReturn(0);

            List<ProjectResponse> result = experienceProjectService.getProjectsByJobExperience(JOB_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("throws ValidationException when job experience missing")
        void throwsWhenJobMissing() {
            when(jobExperienceRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> experienceProjectService.getProjectsByJobExperience(99L))
                    .isInstanceOf(ValidationException.class);
        }
    }

    @Nested
    @DisplayName("getProjectById")
    class GetProjectById {

        @Test
        @DisplayName("returns project with story count")
        void returnsProject() {
            ExperienceProject project = buildProject(10L);
            when(projectRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(project));
            when(storyRepository.countByExperienceProjectIdAndDeletedAtIsNull(10L)).thenReturn(5);

            ProjectResponse result = experienceProjectService.getProjectById(10L);

            assertThat(result.title()).isEqualTo("Auth Service");
            assertThat(result.storyCount()).isEqualTo(5);
        }
    }
}
