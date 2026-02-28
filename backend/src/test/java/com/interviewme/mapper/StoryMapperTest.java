package com.interviewme.mapper;

import com.interviewme.dto.experience.CreateStoryRequest;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.experience.UpdateStoryRequest;
import com.interviewme.model.Story;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class StoryMapperTest {

    @Nested
    @DisplayName("toEntity")
    class ToEntity {

        @Test
        @DisplayName("should map all fields from CreateStoryRequest")
        void shouldMapAllFields() {
            CreateStoryRequest request = new CreateStoryRequest(
                    "Performance Optimization",
                    "Legacy system was slow",
                    "Needed to improve response time",
                    "Refactored database queries",
                    "Reduced latency by 60%",
                    Map.of("latencyReduction", "60%"),
                    "public"
            );

            Story entity = StoryMapper.toEntity(request);

            assertThat(entity.getTitle()).isEqualTo("Performance Optimization");
            assertThat(entity.getSituation()).isEqualTo("Legacy system was slow");
            assertThat(entity.getTask()).isEqualTo("Needed to improve response time");
            assertThat(entity.getAction()).isEqualTo("Refactored database queries");
            assertThat(entity.getResult()).isEqualTo("Reduced latency by 60%");
            assertThat(entity.getMetrics()).containsEntry("latencyReduction", "60%");
            assertThat(entity.getVisibility()).isEqualTo("public");
        }

        @Test
        @DisplayName("should default visibility to private when null")
        void shouldDefaultVisibilityToPrivate() {
            CreateStoryRequest request = new CreateStoryRequest(
                    "Title", "Situation", "Task", "Action", "Result", null, null
            );

            Story entity = StoryMapper.toEntity(request);

            assertThat(entity.getVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("should handle null metrics")
        void shouldHandleNullMetrics() {
            CreateStoryRequest request = new CreateStoryRequest(
                    "Title", "Situation", "Task", "Action", "Result", null, "public"
            );

            Story entity = StoryMapper.toEntity(request);

            assertThat(entity.getMetrics()).isNull();
        }
    }

    @Nested
    @DisplayName("toResponse")
    class ToResponse {

        @Test
        @DisplayName("should map all fields from entity")
        void shouldMapAllFields() {
            Story entity = createTestStory();

            StoryResponse response = StoryMapper.toResponse(entity);

            assertThat(response.id()).isEqualTo(1L);
            assertThat(response.experienceProjectId()).isEqualTo(10L);
            assertThat(response.title()).isEqualTo("Performance Optimization");
            assertThat(response.situation()).isEqualTo("Legacy system was slow");
            assertThat(response.task()).isEqualTo("Needed to improve response time");
            assertThat(response.action()).isEqualTo("Refactored database queries");
            assertThat(response.result()).isEqualTo("Reduced latency by 60%");
            assertThat(response.metrics()).containsEntry("latencyReduction", "60%");
            assertThat(response.visibility()).isEqualTo("public");
        }
    }

    @Nested
    @DisplayName("updateEntity")
    class UpdateEntity {

        @Test
        @DisplayName("should update all fields")
        void shouldUpdateAllFields() {
            Story entity = createTestStory();
            UpdateStoryRequest request = new UpdateStoryRequest(
                    "New Title", "New Situation", "New Task",
                    "New Action", "New Result",
                    Map.of("improvement", "80%"), "private", 0L
            );

            StoryMapper.updateEntity(entity, request);

            assertThat(entity.getTitle()).isEqualTo("New Title");
            assertThat(entity.getSituation()).isEqualTo("New Situation");
            assertThat(entity.getTask()).isEqualTo("New Task");
            assertThat(entity.getAction()).isEqualTo("New Action");
            assertThat(entity.getResult()).isEqualTo("New Result");
            assertThat(entity.getMetrics()).containsEntry("improvement", "80%");
            assertThat(entity.getVisibility()).isEqualTo("private");
        }

        @Test
        @DisplayName("should not update visibility when null")
        void shouldNotUpdateVisibilityWhenNull() {
            Story entity = createTestStory();
            entity.setVisibility("public");
            UpdateStoryRequest request = new UpdateStoryRequest(
                    "Title", "S", "T", "A", "R", null, null, 0L
            );

            StoryMapper.updateEntity(entity, request);

            assertThat(entity.getVisibility()).isEqualTo("public");
        }
    }

    private Story createTestStory() {
        Story story = new Story();
        story.setId(1L);
        story.setTenantId(10L);
        story.setExperienceProjectId(10L);
        story.setTitle("Performance Optimization");
        story.setSituation("Legacy system was slow");
        story.setTask("Needed to improve response time");
        story.setAction("Refactored database queries");
        story.setResult("Reduced latency by 60%");
        story.setMetrics(Map.of("latencyReduction", "60%"));
        story.setVisibility("public");
        story.setCreatedAt(Instant.now());
        story.setUpdatedAt(Instant.now());
        story.setVersion(0L);
        return story;
    }
}
