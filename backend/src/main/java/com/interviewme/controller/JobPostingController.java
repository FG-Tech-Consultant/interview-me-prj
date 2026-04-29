package com.interviewme.controller;

import com.interviewme.dto.job.*;
import com.interviewme.model.User;
import com.interviewme.service.JobPostingAiService;
import com.interviewme.service.JobPostingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Slf4j
public class JobPostingController {

    private final JobPostingService jobPostingService;
    private final JobPostingAiService jobPostingAiService;

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<JobPostingDto> create(
            Authentication authentication,
            @Valid @RequestBody CreateJobPostingRequest request) {
        User user = (User) authentication.getPrincipal();
        JobPostingDto dto = jobPostingService.createJobPosting(
            user.getTenantId(), null, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<List<JobPostingDto>> list(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        List<JobPostingDto> jobs = jobPostingService.listByTenant(user.getTenantId());
        return ResponseEntity.ok(jobs);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<JobPostingDto> getById(
            Authentication authentication,
            @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        JobPostingDto dto = jobPostingService.getById(id, user.getTenantId());
        return ResponseEntity.ok(dto);
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<JobPostingDto> update(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody UpdateJobPostingRequest request) {
        User user = (User) authentication.getPrincipal();
        JobPostingDto dto = jobPostingService.updateJobPosting(id, user.getTenantId(), request);
        return ResponseEntity.ok(dto);
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<Void> delete(
            Authentication authentication,
            @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        jobPostingService.deleteJobPosting(id, user.getTenantId());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/ai/generate-description")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<AiJobDescriptionResponse> generateDescription(
            Authentication authentication,
            @Valid @RequestBody AiJobDescriptionRequest request) {
        User user = (User) authentication.getPrincipal();
        AiJobDescriptionResponse response = jobPostingAiService.generateJobDescription(
            user.getTenantId(), request);
        return ResponseEntity.ok(response);
    }
}
