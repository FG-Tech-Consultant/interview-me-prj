package com.interviewme.service;

import com.interviewme.common.exception.OptimisticLockException;
import com.interviewme.common.exception.ProjectNotFoundException;
import com.interviewme.common.exception.StoryNotFoundException;
import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.experience.CreateStoryRequest;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.experience.UpdateStoryRequest;
import com.interviewme.event.ContentChangedEventListener;
import com.interviewme.model.ExperienceProject;
import com.interviewme.model.Story;
import com.interviewme.repository.ExperienceProjectRepository;
import com.interviewme.repository.StoryRepository;
import com.interviewme.repository.StorySkillRepository;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoryService")
class StoryServiceTest {

    @Mock private StoryRepository storyRepository;
    @Mock private StorySkillRepository storySkillRepository;
    @Mock private ExperienceProjectRepository projectRepository;
    @Mock private ContentChangedEventListener contentChangedEventListener;

    @InjectMocks
    private StoryService storyService;

    private MockedStatic<TenantContext> tenantContextMock;

    private static final Long TENANT_ID = 100L;
    private static final Long PROJECT_ID = 5L;

    @BeforeEach
    void setUp() {
        tenantContextMock = mockStatic(TenantContext.class);
        tenantContextMock.when(TenantContext::getCurrentTenantId).thenReturn(TENANT_ID);
    }

    @AfterEach
    void tearDown() {
        tenantContextMock.close();
    }

    private Story buildStory(Long id) {
        Story s = new Story();
        s.setId(id);
        s.setTenantId(TENANT_ID);
        s.setExperienceProjectId(PROJECT_ID);
        s.setTitle("Led migration");
        s.setSituation("Legacy system");
        s.setTask("Migrate to microservices");
        s.setAction("Designed architecture");
        s.setResult("50% latency reduction");
        s.setVisibility("private");
        return s;
    }

    @Nested
    @DisplayName("createStory")
    class CreateStory {

        @Test
        @DisplayName("creates story when project exists")
        void createsStorySuccessfully() {
            ExperienceProject project = new ExperienceProject();
            project.setId(PROJECT_ID);
            when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));

            Story saved = buildStory(20L);
            when(storyRepository.save(any(Story.class))).thenReturn(saved);

            CreateStoryRequest request = new CreateStoryRequest(
                    "Led migration", "Legacy system", "Migrate", "Designed", "50% reduction",
                    Map.of("latency", "50%"), "private");

            StoryResponse result = storyService.createStory(PROJECT_ID, request);

            assertThat(result).isNotNull();
            assertThat(result.title()).isEqualTo("Led migration");
            verify(storyRepository).save(argThat(s -> s.getTenantId().equals(TENANT_ID)));
        }

        @Test
        @DisplayName("throws ProjectNotFoundException when project missing")
        void throwsWhenProjectMissing() {
            when(projectRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            CreateStoryRequest request = new CreateStoryRequest(
                    "Title", "Sit", "Task", "Action", "Result", null, null);

            assertThatThrownBy(() -> storyService.createStory(99L, request))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("updateStory")
    class UpdateStory {

        @Test
        @DisplayName("updates story successfully")
        void updatesSuccessfully() {
            Story existing = buildStory(20L);
            when(storyRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(existing));
            when(storyRepository.save(any(Story.class))).thenReturn(existing);

            UpdateStoryRequest request = new UpdateStoryRequest(
                    "Updated title", "Sit", "Task", "Action", "Result", null, "public", 1L);

            StoryResponse result = storyService.updateStory(20L, request);

            assertThat(result).isNotNull();
        }

        @Test
        @DisplayName("throws StoryNotFoundException when story missing")
        void throwsWhenMissing() {
            when(storyRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> storyService.updateStory(99L,
                    new UpdateStoryRequest("T", "S", "T", "A", "R", null, null, 1L)))
                    .isInstanceOf(StoryNotFoundException.class);
        }

        @Test
        @DisplayName("throws OptimisticLockException on concurrent modification")
        void throwsOnOptimisticLock() {
            Story existing = buildStory(20L);
            when(storyRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(existing));
            when(storyRepository.save(any(Story.class)))
                    .thenThrow(new OptimisticLockingFailureException("conflict"));

            assertThatThrownBy(() -> storyService.updateStory(20L,
                    new UpdateStoryRequest("T", "S", "T", "A", "R", null, null, 1L)))
                    .isInstanceOf(OptimisticLockException.class);
        }
    }

    @Nested
    @DisplayName("deleteStory")
    class DeleteStory {

        @Test
        @DisplayName("soft deletes story and cleans skill associations")
        void softDeletesStory() {
            Story story = buildStory(20L);
            when(storyRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(story));
            when(storyRepository.save(any(Story.class))).thenReturn(story);

            storyService.deleteStory(20L);

            assertThat(story.getDeletedAt()).isNotNull();
            verify(storySkillRepository).deleteByStoryId(20L);
            verify(storyRepository).save(story);
        }

        @Test
        @DisplayName("throws StoryNotFoundException when story missing")
        void throwsWhenMissing() {
            when(storyRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> storyService.deleteStory(99L))
                    .isInstanceOf(StoryNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getStoriesByProject")
    class GetStoriesByProject {

        @Test
        @DisplayName("returns stories for existing project")
        void returnsStoriesForProject() {
            ExperienceProject project = new ExperienceProject();
            project.setId(PROJECT_ID);
            when(projectRepository.findByIdAndDeletedAtIsNull(PROJECT_ID)).thenReturn(Optional.of(project));

            Story s1 = buildStory(20L);
            Story s2 = buildStory(21L);
            when(storyRepository.findByExperienceProjectIdAndDeletedAtIsNullOrderByCreatedAtDesc(PROJECT_ID))
                    .thenReturn(List.of(s1, s2));

            List<StoryResponse> result = storyService.getStoriesByProject(PROJECT_ID);

            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("throws ProjectNotFoundException when project missing")
        void throwsWhenProjectMissing() {
            when(projectRepository.findByIdAndDeletedAtIsNull(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> storyService.getStoriesByProject(99L))
                    .isInstanceOf(ProjectNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("getStoryById")
    class GetStoryById {

        @Test
        @DisplayName("returns story when found")
        void returnsStory() {
            Story story = buildStory(20L);
            when(storyRepository.findByIdAndDeletedAtIsNull(20L)).thenReturn(Optional.of(story));

            StoryResponse result = storyService.getStoryById(20L);

            assertThat(result.title()).isEqualTo("Led migration");
        }
    }

    @Nested
    @DisplayName("getPublicStoriesByProfile")
    class GetPublicStories {

        @Test
        @DisplayName("returns public stories for profile")
        void returnsPublicStories() {
            Story s1 = buildStory(20L);
            s1.setVisibility("public");
            when(storyRepository.findPublicStoriesByProfileId(10L)).thenReturn(List.of(s1));

            List<StoryResponse> result = storyService.getPublicStoriesByProfile(10L);

            assertThat(result).hasSize(1);
        }
    }
}
