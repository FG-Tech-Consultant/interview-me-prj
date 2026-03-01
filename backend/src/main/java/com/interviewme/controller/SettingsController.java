package com.interviewme.controller;

import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.settings.AiSettingsRequest;
import com.interviewme.dto.settings.AiSettingsResponse;
import com.interviewme.service.TenantSettingsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings")
@RequiredArgsConstructor
@Slf4j
public class SettingsController {

    private final TenantSettingsService tenantSettingsService;

    @GetMapping("/ai")
    @Transactional(readOnly = true)
    public ResponseEntity<AiSettingsResponse> getAiSettings() {
        Long tenantId = TenantContext.getTenantId();
        log.info("GET /api/settings/ai - tenantId: {}", tenantId);
        return ResponseEntity.ok(tenantSettingsService.getAiSettings(tenantId));
    }

    @PutMapping("/ai")
    @Transactional
    public ResponseEntity<AiSettingsResponse> updateAiSettings(@RequestBody AiSettingsRequest request) {
        Long tenantId = TenantContext.getTenantId();
        log.info("PUT /api/settings/ai - tenantId: {} provider: {} model: {}",
                tenantId, request.provider(), request.chatModel());
        tenantSettingsService.updateAiSettings(tenantId, request.provider(), request.chatModel());
        return ResponseEntity.ok(tenantSettingsService.getAiSettings(tenantId));
    }
}
