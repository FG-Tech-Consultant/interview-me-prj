package com.interviewme.controller;

import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.visitor.*;
import com.interviewme.model.Profile;
import com.interviewme.repository.ProfileRepository;
import com.interviewme.service.VisitorService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
@Slf4j
public class VisitorController {

    private final VisitorService visitorService;
    private final ProfileRepository profileRepository;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<Page<VisitorResponse>> getMyVisitors(Pageable pageable) {
        Long tenantId = TenantContext.getTenantId();
        Profile profile = profileRepository.findByTenantId(tenantId).orElse(null);
        if (profile == null) return ResponseEntity.ok(Page.empty());
        Page<VisitorResponse> visitors = visitorService.getVisitorsByProfile(profile.getId(), tenantId, pageable);
        return ResponseEntity.ok(visitors);
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public ResponseEntity<VisitorStatsResponse> getVisitorStats() {
        Long tenantId = TenantContext.getTenantId();
        Profile profile = profileRepository.findByTenantId(tenantId).orElse(null);
        if (profile == null) return ResponseEntity.ok(new VisitorStatsResponse(0, 0, 0));
        VisitorStatsResponse stats = visitorService.getVisitorStats(tenantId, profile.getId());
        return ResponseEntity.ok(stats);
    }

    @GetMapping("/{visitorId}/sessions")
    @Transactional(readOnly = true)
    public ResponseEntity<List<VisitorSessionResponse>> getVisitorSessions(@PathVariable Long visitorId) {
        List<VisitorSessionResponse> sessions = visitorService.getVisitorSessions(visitorId);
        return ResponseEntity.ok(sessions);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    @Transactional(readOnly = true)
    public ResponseEntity<List<VisitorChatLogResponse>> getSessionMessages(@PathVariable Long sessionId) {
        List<VisitorChatLogResponse> messages = visitorService.getSessionMessages(sessionId);
        return ResponseEntity.ok(messages);
    }

    @PostMapping("/{visitorId}/reveal")
    @Transactional
    public ResponseEntity<VisitorResponse> revealContact(@PathVariable Long visitorId) {
        Long tenantId = TenantContext.getTenantId();
        VisitorResponse visitor = visitorService.revealContact(tenantId, visitorId);
        return ResponseEntity.ok(visitor);
    }
}
