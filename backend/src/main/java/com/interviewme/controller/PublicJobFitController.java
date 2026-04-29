package com.interviewme.controller;

import com.interviewme.dto.job.JobFitChatRequest;
import com.interviewme.dto.job.JobFitChatResponse;
import com.interviewme.dto.job.JobPostingDto;
import com.interviewme.model.JobPosting;
import com.interviewme.service.JobFitChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/jobs")
@RequiredArgsConstructor
@Slf4j
public class PublicJobFitController {

    private final JobFitChatService jobFitChatService;

    /**
     * GET /api/public/jobs/{slug} — view public job posting details (no auth required)
     */
    @GetMapping("/{slug}")
    @Transactional(readOnly = true)
    public ResponseEntity<JobPostingDto> getJobPosting(@PathVariable String slug) {
        JobPosting job = jobFitChatService.getActiveJobBySlug(slug);
        return ResponseEntity.ok(toDto(job));
    }

    /**
     * POST /api/public/jobs/{slug}/chat — chat with AI about job fit (no auth required)
     */
    @PostMapping("/{slug}/chat")
    @Transactional
    public ResponseEntity<JobFitChatResponse> chat(
            @PathVariable String slug,
            @Valid @RequestBody JobFitChatRequest request) {
        log.info("Job fit chat slug={} messageLength={}", slug,
            request.message() != null ? request.message().length() : 0);
        JobFitChatResponse response = jobFitChatService.chat(slug, request);
        return ResponseEntity.ok(response);
    }

    private JobPostingDto toDto(JobPosting job) {
        return new JobPostingDto(
            job.getId(),
            job.getTitle(),
            job.getSlug(),
            job.getDescription(),
            job.getRequirements(),
            job.getBenefits(),
            job.getLocation(),
            job.getWorkModel(),
            job.getSalaryRange(),
            job.getExperienceLevel(),
            job.getStatus().name(),
            job.getRequiredSkills(),
            job.getNiceToHaveSkills(),
            job.getCreatedAt()
        );
    }
}
