package com.interviewme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.dto.experience.CreateStoryRequest;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.experience.UpdateStoryRequest;
import com.interviewme.service.StoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StoryController")
class StoryControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private StoryService storyService;

    @InjectMocks
    private StoryController storyController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storyController).build();
    }

    private StoryResponse sampleStory() {
        return new StoryResponse(
                1L, 5L, "Migrated to Microservices",
                "Legacy monolith was slowing development",
                "Decompose into 12 bounded-context services",
                "Led architecture design and migration plan",
                "Reduced deploy time from 4h to 15min",
                Map.of("deployTime", "15min"), "public",
                Instant.now(), Instant.now(), 1L);
    }

    @Nested
    @DisplayName("GET /api/v1/projects/{projectId}/stories")
    class GetStoriesByProject {

        @Test
        @DisplayName("returns list of stories")
        void success() throws Exception {
            when(storyService.getStoriesByProject(5L)).thenReturn(List.of(sampleStory()));

            mockMvc.perform(get("/api/v1/projects/5/stories"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Migrated to Microservices"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/projects/{projectId}/stories")
    class CreateStory {

        @Test
        @DisplayName("returns 201 on success")
        void success() throws Exception {
            when(storyService.createStory(eq(5L), any(CreateStoryRequest.class))).thenReturn(sampleStory());

            CreateStoryRequest request = new CreateStoryRequest(
                    "Migrated to Microservices",
                    "Legacy monolith",
                    "Decompose services",
                    "Led architecture",
                    "Reduced deploy time",
                    null, "public");

            mockMvc.perform(post("/api/v1/projects/5/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Migrated to Microservices"));
        }

        @Test
        @DisplayName("returns 400 when title is blank")
        void blankTitle() throws Exception {
            CreateStoryRequest request = new CreateStoryRequest(
                    "", "situation", "task", "action", "result", null, null);

            mockMvc.perform(post("/api/v1/projects/5/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when situation is blank")
        void blankSituation() throws Exception {
            CreateStoryRequest request = new CreateStoryRequest(
                    "Title", "", "task", "action", "result", null, null);

            mockMvc.perform(post("/api/v1/projects/5/stories")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/stories/{storyId}")
    class GetStory {

        @Test
        @DisplayName("returns story by id")
        void success() throws Exception {
            when(storyService.getStoryById(1L)).thenReturn(sampleStory());

            mockMvc.perform(get("/api/v1/stories/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/stories/{storyId}")
    class UpdateStory {

        @Test
        @DisplayName("returns 200 on success")
        void success() throws Exception {
            when(storyService.updateStory(eq(1L), any(UpdateStoryRequest.class))).thenReturn(sampleStory());

            UpdateStoryRequest request = new UpdateStoryRequest(
                    "Updated Title", "situation", "task", "action", "result",
                    null, "public", 1L);

            mockMvc.perform(put("/api/v1/stories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.title").value("Migrated to Microservices"));
        }

        @Test
        @DisplayName("returns 400 when version is null")
        void missingVersion() throws Exception {
            UpdateStoryRequest request = new UpdateStoryRequest(
                    "Title", "situation", "task", "action", "result",
                    null, "public", null);

            mockMvc.perform(put("/api/v1/stories/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/stories/{storyId}")
    class DeleteStory {

        @Test
        @DisplayName("returns 204 on success")
        void success() throws Exception {
            doNothing().when(storyService).deleteStory(1L);

            mockMvc.perform(delete("/api/v1/stories/1"))
                    .andExpect(status().isNoContent());

            verify(storyService).deleteStory(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/profiles/{profileId}/stories/public")
    class GetPublicStories {

        @Test
        @DisplayName("returns public stories")
        void success() throws Exception {
            when(storyService.getPublicStoriesByProfile(10L)).thenReturn(List.of(sampleStory()));

            mockMvc.perform(get("/api/v1/profiles/10/stories/public"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
