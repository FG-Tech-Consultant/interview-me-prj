package com.interviewme.controller;

import com.interviewme.dto.linkedin.CreateDraftRequest;
import com.interviewme.dto.linkedin.DraftPageResponse;
import com.interviewme.dto.linkedin.DraftResponse;
import com.interviewme.dto.linkedin.UpdateDraftStatusRequest;
import com.interviewme.model.DraftStatus;
import com.interviewme.service.LinkedInDraftService;
import com.interviewme.service.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/linkedin/drafts")
@RequiredArgsConstructor
@Slf4j
public class LinkedInDraftController {

    private final LinkedInDraftService draftService;
    private final ProfileService profileService;

    @PostMapping
    @Transactional
    public ResponseEntity<DraftResponse> createDraft(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreateDraftRequest request) {
        log.info("POST /api/linkedin/drafts");
        Long profileId = getProfileId(userDetails);
        DraftResponse response = draftService.createDraft(profileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<DraftPageResponse> listDrafts(
            @AuthenticationPrincipal UserDetails userDetails,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        log.info("GET /api/linkedin/drafts");
        Long profileId = getProfileId(userDetails);
        DraftPageResponse response = draftService.listDrafts(profileId, page, size);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<DraftResponse> getDraft(@PathVariable Long id) {
        log.info("GET /api/linkedin/drafts/{}", id);
        DraftResponse response = draftService.getDraft(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deleteDraft(@PathVariable Long id) {
        log.info("DELETE /api/linkedin/drafts/{}", id);
        draftService.deleteDraft(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/status")
    @Transactional
    public ResponseEntity<DraftResponse> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateDraftStatusRequest request) {
        log.info("PUT /api/linkedin/drafts/{}/status -> {}", id, request.status());
        DraftStatus newStatus = DraftStatus.valueOf(request.status());
        DraftResponse response = draftService.updateStatus(id, newStatus);
        return ResponseEntity.ok(response);
    }

    private Long getProfileId(UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return profileService.getProfileByUserId(userId).id();
    }
}
