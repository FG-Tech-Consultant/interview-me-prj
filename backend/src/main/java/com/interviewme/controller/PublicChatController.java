package com.interviewme.controller;

import com.interviewme.aichat.dto.ChatRequest;
import com.interviewme.aichat.dto.ChatResponse;
import com.interviewme.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/chat")
@RequiredArgsConstructor
@Slf4j
public class PublicChatController {

    private final ChatService chatService;

    @PostMapping("/{slug}/messages")
    @Transactional
    public ResponseEntity<ChatResponse> sendMessage(
            @PathVariable String slug,
            @Valid @RequestBody ChatRequest request) {
        log.info("Chat message received slug={} messageLength={}", slug,
                request.message() != null ? request.message().length() : 0);

        ChatResponse response = chatService.processMessage(slug, request);
        return ResponseEntity.ok(response);
    }
}
