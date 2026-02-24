package com.interviewme.controller;

import com.interviewme.aichat.dto.ChatAnalyticsResponse;
import com.interviewme.service.ChatService;
import com.interviewme.common.util.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Slf4j
public class ChatAnalyticsController {

    private final ChatService chatService;

    @GetMapping("/analytics")
    @Transactional(readOnly = true)
    public ResponseEntity<ChatAnalyticsResponse> getAnalytics() {
        Long tenantId = TenantContext.getTenantId();
        ChatAnalyticsResponse analytics = chatService.getAnalytics(tenantId);
        return ResponseEntity.ok(analytics);
    }
}
