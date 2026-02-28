package com.interviewme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.interviewme.dto.skill.AddUserSkillDto;
import com.interviewme.dto.skill.SkillDto;
import com.interviewme.dto.skill.UpdateUserSkillDto;
import com.interviewme.dto.skill.UserSkillDto;
import com.interviewme.service.UserSkillService;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserSkillController")
class UserSkillControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());

    @Mock
    private UserSkillService userSkillService;

    @InjectMocks
    private UserSkillController userSkillController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userSkillController).build();
    }

    private UserSkillDto sampleUserSkill() {
        return new UserSkillDto(
                1L, new SkillDto(1L, "Java", "Languages", "desc", List.of(), true),
                5, 4, LocalDate.of(2025, 1, 1), "high",
                List.of("backend"), "public", Instant.now(), Instant.now());
    }

    @Nested
    @DisplayName("GET /api/v1/skills/user/{profileId}")
    class GetUserSkills {

        @Test
        @DisplayName("returns skills grouped by category")
        void success() throws Exception {
            Map<String, List<UserSkillDto>> skills = Map.of("Languages", List.of(sampleUserSkill()));
            when(userSkillService.getSkillsByProfile(10L)).thenReturn(skills);

            mockMvc.perform(get("/api/v1/skills/user/10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.Languages").isArray())
                    .andExpect(jsonPath("$.Languages.length()").value(1));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/skills/user/{profileId}")
    class AddUserSkill {

        @Test
        @DisplayName("returns 201 on success")
        void success() throws Exception {
            when(userSkillService.addSkill(eq(10L), any(AddUserSkillDto.class))).thenReturn(sampleUserSkill());

            mockMvc.perform(post("/api/v1/skills/user/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AddUserSkillDto(1L, 5, 4, LocalDate.of(2025, 1, 1), "high", List.of(), "public"))))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.skill.name").value("Java"));
        }

        @Test
        @DisplayName("returns 400 when skillId is null")
        void missingSkillId() throws Exception {
            mockMvc.perform(post("/api/v1/skills/user/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"proficiencyDepth\":3}"))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("returns 400 when proficiencyDepth exceeds max")
        void invalidProficiency() throws Exception {
            mockMvc.perform(post("/api/v1/skills/user/10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new AddUserSkillDto(1L, 5, 10, null, null, null, null))))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/skills/user/detail/{id}")
    class GetUserSkillById {

        @Test
        @DisplayName("returns skill detail")
        void success() throws Exception {
            when(userSkillService.getSkillById(1L)).thenReturn(sampleUserSkill());

            mockMvc.perform(get("/api/v1/skills/user/detail/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.skill.name").value("Java"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/skills/user/detail/{id}")
    class UpdateUserSkill {

        @Test
        @DisplayName("returns 200 on success")
        void success() throws Exception {
            when(userSkillService.updateSkill(eq(1L), any(UpdateUserSkillDto.class))).thenReturn(sampleUserSkill());

            mockMvc.perform(put("/api/v1/skills/user/detail/1")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(
                                    new UpdateUserSkillDto(6, 5, null, "expert", List.of(), "public"))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1));
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/skills/user/detail/{id}")
    class DeleteUserSkill {

        @Test
        @DisplayName("returns 204 on success")
        void success() throws Exception {
            doNothing().when(userSkillService).deleteSkill(1L);

            mockMvc.perform(delete("/api/v1/skills/user/detail/1"))
                    .andExpect(status().isNoContent());

            verify(userSkillService).deleteSkill(1L);
        }
    }

    @Nested
    @DisplayName("GET /api/v1/skills/user/{profileId}/public")
    class GetPublicSkills {

        @Test
        @DisplayName("returns public skills list")
        void success() throws Exception {
            when(userSkillService.getPublicSkillsByProfile(10L)).thenReturn(List.of(sampleUserSkill()));

            mockMvc.perform(get("/api/v1/skills/user/10/public"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}
