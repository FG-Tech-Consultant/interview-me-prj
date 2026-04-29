package com.interviewme.controller;

import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.matching.MatchRequest;
import com.interviewme.dto.matching.MatchResponse;
import com.interviewme.service.MatchingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/matching")
@RequiredArgsConstructor
public class MatchingController {

    private final MatchingService matchingService;

    @PostMapping("/candidates")
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    @Transactional(readOnly = true)
    public ResponseEntity<MatchResponse> matchCandidates(@Valid @RequestBody MatchRequest request) {
        Long tenantId = TenantContext.getCurrentTenantId();
        MatchResponse response = matchingService.matchCandidates(tenantId, request);
        return ResponseEntity.ok(response);
    }
}
