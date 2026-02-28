package com.interviewme.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewme.aichat.dto.ChatRequest;
import com.interviewme.aichat.dto.ChatResponse;
import com.interviewme.aichat.dto.QuotaInfo;
import com.interviewme.service.ChatService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PublicChatController")
class PublicChatControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ChatService chatService;

    @InjectMocks
    private PublicChatController publicChatController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(publicChatController).build();
    }

    @Test
    @DisplayName("POST /api/public/chat/{slug}/messages - success")
    void sendMessage_Success() throws Exception {
        UUID sessionToken = UUID.randomUUID();
        ChatRequest request = new ChatRequest("Hello, tell me about your experience", sessionToken);
        ChatResponse response = new ChatResponse(
                "I have 5 years of experience...", sessionToken, 1L,
                new QuotaInfo(10, 9, false));

        when(chatService.processMessage(eq("john-doe"), any(ChatRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/public/chat/john-doe/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("I have 5 years of experience..."))
                .andExpect(jsonPath("$.sessionToken").value(sessionToken.toString()))
                .andExpect(jsonPath("$.messageId").value(1));

        verify(chatService).processMessage(eq("john-doe"), any(ChatRequest.class));
    }

    @Test
    @DisplayName("POST /api/public/chat/{slug}/messages - validation fails on blank message")
    void sendMessage_BlankMessage() throws Exception {
        ChatRequest request = new ChatRequest("", null);

        mockMvc.perform(post("/api/public/chat/john-doe/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatService);
    }

    @Test
    @DisplayName("POST /api/public/chat/{slug}/messages - service throws exception propagates")
    void sendMessage_ServiceError() throws Exception {
        ChatRequest request = new ChatRequest("Hello", null);

        when(chatService.processMessage(eq("unknown-slug"), any(ChatRequest.class)))
                .thenThrow(new RuntimeException("Profile not found"));

        assertThrows(Exception.class, () ->
                mockMvc.perform(post("/api/public/chat/unknown-slug/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))));
    }
}
