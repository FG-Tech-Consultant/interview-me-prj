package com.interviewme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.dto.experience.CreateProjectRequest;
import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.UpdateProjectRequest;
import com.interviewme.service.ExperienceProjectService;
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
@DisplayName("ExperienceProjectController")
class ExperienceProjectControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ExperienceProjectService projectService;

    @InjectMocks
    private ExperienceProjectController projectController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(projectController).build();
    }

    private ProjectResponse sampleProject() {
        return new ProjectResponse(
                1L, 5L, "Payment Gateway", "Built high-scale gateway",
                "Tech Lead", 8, List.of("Java", "Kafka"),
                "Microservices", Map.of("tps", "50000"),
                "Reduced processing time by 60%", "public", 3,
                Instant.now(), Instant.now(), 1L);
    }

    @Nested
    @DisplayName("GET /api/v1/jobs/{jobId}/projects")
    class GetProjectsByJob {

        @Test
        @DisplayName("returns list of projects")
        void success() throws Exception {
            when(projectService.getProjectsByJobExperience(5L)).thenReturn(List.of(sampleProject()));

            mockMvc.perform(get("/api/v1/jobs/5/projects"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].title").value("Payment Gateway"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/jobs/{jobId}/projects")
    class CreateProject {

        @Test
        @DisplayName("returns 201 on success")
        void success() throws Exception {
            when(projectService.createProject(eq(5L), any(CreateProjectRequest.class))).thenReturn(sampleProject());

            CreateProjectRequest request = new CreateProjectRequest(
                    "Payment Gateway", "Built high-scale gateway", "Tech Lead",
                    8, List.of("Java", "Kafka"), "Microservices",
                    Map.of("tps", "50000"), "Reduced processing time by 60%", "public");

            mockMvc.perform(post("/api/v1/jobs/5/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.title").value("Payment Gateway"));
        }

        @Test
        @DisplayName("returns 400 when title is blank")
        void blankTitle() throws Exception {
            CreateProjectRequest request = new CreateProjectRequest(
                    "", null, null, null, null, null, null, null, null);

            mockMvc.perform(post("/api/v1/jobs/5/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when teamSize exceeds 1000")
        void invalidTeamSize() throws Exception {
            CreateProjectRequest request = new CreateProjectRequest(
                    "Title", null, null, 5000, null, null, null, null, null);

            mockMvc.perform(post("/api/v1/jobs/5/projects")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/projects/{projectId}")
    class GetProject {

        @Test
        @DisplayName("returns project by id")
        void success() throws Exception {
            when(projectService.getProjectById(1L)).thenReturn(sampleProject());

            mockMvc.perform(get("/api/v1/projects/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.title").value("Payment Gateway"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/projects/{projectId}")
    class UpdateProject {

        @Test
        @DisplayName("returns 200 on success")
        void success() throws Exception {
            when(projectService.updateProject(eq(1L), any(UpdateProjectRequest.class))).thenReturn(sampleProject());

            UpdateProjectRequest request = new UpdateProjectRequest(
                    "Updated Gateway", "New context", "Lead", 10,
                    List.of("Java"), "Microservices", null, "outcomes", "public", 1L);

            mockMvc.perform(put("/api/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("returns 400 when version is null")
        void missingVersion() throws Exception {
            UpdateProjectRequest request = new UpdateProjectRequest(
                    "Title", null, null, null, null, null, null, null, null, null);

            mockMvc.perform(put("/api/v1/projects/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/projects/{projectId}")
    class DeleteProject {

        @Test
        @DisplayName("returns 204 on success")
        void success() throws Exception {
            doNothing().when(projectService).deleteProject(1L);

            mockMvc.perform(delete("/api/v1/projects/1"))
                    .andExpect(status().isNoContent());

            verify(projectService).deleteProject(1L);
        }
    }
}
