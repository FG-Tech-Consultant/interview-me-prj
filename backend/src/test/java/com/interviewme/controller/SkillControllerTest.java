package com.interviewme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.dto.skill.CreateSkillDto;
import com.interviewme.dto.skill.SkillDto;
import com.interviewme.dto.skill.UpdateSkillDto;
import com.interviewme.service.SkillService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SkillController")
class SkillControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SkillService skillService;

    @InjectMocks
    private SkillController skillController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(skillController).build();
    }

    private SkillDto sampleSkill() {
        return new SkillDto(1L, "Java", "Languages", "Java programming language", List.of("backend"), true);
    }

    @Nested
    @DisplayName("GET /api/v1/skills/catalog/search")
    class SearchCatalog {

        @Test
        @DisplayName("returns matching skills")
        void success() throws Exception {
            when(skillService.searchActive("java")).thenReturn(List.of(sampleSkill()));

            mockMvc.perform(get("/api/v1/skills/catalog/search").param("q", "java"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Java"));
        }

        @Test
        @DisplayName("returns empty list for no match")
        void noMatch() throws Exception {
            when(skillService.searchActive("nonexistent")).thenReturn(List.of());

            mockMvc.perform(get("/api/v1/skills/catalog/search").param("q", "nonexistent"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }

        @Test
        @DisplayName("returns 400 when q parameter is missing")
        void missingQuery() throws Exception {
            mockMvc.perform(get("/api/v1/skills/catalog/search"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/skills/catalog")
    class GetAllSkills {

        @Test
        @DisplayName("returns all skills")
        void success() throws Exception {
            when(skillService.findAll()).thenReturn(List.of(sampleSkill()));

            mockMvc.perform(get("/api/v1/skills/catalog"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/skills/catalog")
    class CreateSkill {

        @Test
        @DisplayName("returns 201 on success")
        void success() throws Exception {
            when(skillService.createSkill(any(CreateSkillDto.class))).thenReturn(sampleSkill());

            mockMvc.perform(post("/api/v1/skills/catalog")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateSkillDto("Java", "Languages", "Java language", List.of()))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.name").value("Java"));
        }

        @Test
        @DisplayName("returns 400 when name is blank")
        void blankName() throws Exception {
            mockMvc.perform(post("/api/v1/skills/catalog")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateSkillDto("", "Languages", null, null))))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when category is blank")
        void blankCategory() throws Exception {
            mockMvc.perform(post("/api/v1/skills/catalog")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new CreateSkillDto("Java", "", null, null))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/skills/catalog/{id}")
    class UpdateSkill {

        @Test
        @DisplayName("returns 200 on success")
        void success() throws Exception {
            when(skillService.updateSkill(eq(1L), any(UpdateSkillDto.class))).thenReturn(sampleSkill());

            mockMvc.perform(put("/api/v1/skills/catalog/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateSkillDto("Java 21", "Languages", "Updated", List.of()))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Java"));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/skills/catalog/{id}/deactivate")
    class DeactivateSkill {

        @Test
        @DisplayName("deactivates skill")
        void success() throws Exception {
            SkillDto deactivated = new SkillDto(1L, "Java", "Languages", "desc", List.of(), false);
            when(skillService.deactivateSkill(1L)).thenReturn(deactivated);

            mockMvc.perform(post("/api/v1/skills/catalog/1/deactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(false));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/skills/catalog/{id}/reactivate")
    class ReactivateSkill {

        @Test
        @DisplayName("reactivates skill")
        void success() throws Exception {
            when(skillService.reactivateSkill(1L)).thenReturn(sampleSkill());

            mockMvc.perform(post("/api/v1/skills/catalog/1/reactivate"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.isActive").value(true));
        }
    }
}
