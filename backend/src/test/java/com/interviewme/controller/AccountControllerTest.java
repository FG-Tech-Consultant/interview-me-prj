package com.interviewme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.model.User;
import com.interviewme.service.AccountDeletionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountController")
class AccountControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private AccountDeletionService accountDeletionService;

    @InjectMocks
    private AccountController accountController;

    private User testUser;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(accountController).build();
        testUser = new User();
        testUser.setId(1L);
        testUser.setTenantId(100L);
        testUser.setEmail("test@example.com");
        authentication = new UsernamePasswordAuthenticationToken(testUser, null, testUser.getAuthorities());
    }

    @Test
    @DisplayName("DELETE /api/v1/account - success with correct confirmation")
    void deleteAccount_Success() throws Exception {
        Map<String, Integer> deletedCounts = Map.of("profiles", 1, "jobs", 3, "skills", 5);
        when(accountDeletionService.deleteAccount(100L, 1L)).thenReturn(deletedCounts);

        mockMvc.perform(delete("/api/v1/account")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmation", "DELETE MY ACCOUNT"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Account and all associated data have been permanently deleted."))
                .andExpect(jsonPath("$.deletedCounts").exists());

        verify(accountDeletionService).deleteAccount(100L, 1L);
    }

    @Test
    @DisplayName("DELETE /api/v1/account - returns 400 with wrong confirmation")
    void deleteAccount_WrongConfirmation() throws Exception {
        mockMvc.perform(delete("/api/v1/account")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("confirmation", "wrong text"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CONFIRMATION_REQUIRED"));

        verifyNoInteractions(accountDeletionService);
    }

    @Test
    @DisplayName("DELETE /api/v1/account - returns 400 with missing confirmation")
    void deleteAccount_MissingConfirmation() throws Exception {
        mockMvc.perform(delete("/api/v1/account")
                        .principal(authentication)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("CONFIRMATION_REQUIRED"));

        verifyNoInteractions(accountDeletionService);
    }
}
