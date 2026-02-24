package com.interviewme.controller;

import com.interviewme.dto.job.CreateJobExperienceRequest;
import com.interviewme.dto.job.JobExperienceResponse;
import com.interviewme.dto.job.UpdateJobExperienceRequest;
import com.interviewme.service.JobExperienceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles/{profileId}/job-experiences")
@RequiredArgsConstructor
@Slf4j
public class JobExperienceController {

    private final JobExperienceService jobExperienceService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<JobExperienceResponse>> getJobExperiences(@PathVariable Long profileId) {
        log.info("GET /api/profiles/{}/job-experiences", profileId);

        List<JobExperienceResponse> experiences = jobExperienceService.getJobExperiencesByProfileId(profileId);
        return ResponseEntity.ok(experiences);
    }

    @GetMapping("/{experienceId}")
    @Transactional(readOnly = true)
    public ResponseEntity<JobExperienceResponse> getJobExperienceById(
            @PathVariable Long profileId,
            @PathVariable Long experienceId) {
        log.info("GET /api/profiles/{}/job-experiences/{}", profileId, experienceId);

        JobExperienceResponse experience = jobExperienceService.getJobExperienceById(profileId, experienceId);
        return ResponseEntity.ok(experience);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<JobExperienceResponse> createJobExperience(
            @PathVariable Long profileId,
            @Valid @RequestBody CreateJobExperienceRequest request) {
        log.info("POST /api/profiles/{}/job-experiences", profileId);

        JobExperienceResponse experience = jobExperienceService.createJobExperience(profileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(experience);
    }

    @PutMapping("/{experienceId}")
    @Transactional
    public ResponseEntity<JobExperienceResponse> updateJobExperience(
            @PathVariable Long profileId,
            @PathVariable Long experienceId,
            @Valid @RequestBody UpdateJobExperienceRequest request) {
        log.info("PUT /api/profiles/{}/job-experiences/{}", profileId, experienceId);

        JobExperienceResponse experience = jobExperienceService.updateJobExperience(profileId, experienceId, request);
        return ResponseEntity.ok(experience);
    }

    @DeleteMapping("/{experienceId}")
    @Transactional
    public ResponseEntity<Void> deleteJobExperience(
            @PathVariable Long profileId,
            @PathVariable Long experienceId) {
        log.info("DELETE /api/profiles/{}/job-experiences/{}", profileId, experienceId);

        jobExperienceService.deleteJobExperience(profileId, experienceId);
        return ResponseEntity.noContent().build();
    }
}
