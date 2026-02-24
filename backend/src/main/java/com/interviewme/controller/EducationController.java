package com.interviewme.controller;

import com.interviewme.dto.education.CreateEducationRequest;
import com.interviewme.dto.education.EducationResponse;
import com.interviewme.dto.education.UpdateEducationRequest;
import com.interviewme.service.EducationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/profiles/{profileId}/education")
@RequiredArgsConstructor
@Slf4j
public class EducationController {

    private final EducationService educationService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<EducationResponse>> getEducations(@PathVariable Long profileId) {
        log.info("GET /api/profiles/{}/education", profileId);

        List<EducationResponse> educations = educationService.getEducationsByProfileId(profileId);
        return ResponseEntity.ok(educations);
    }

    @GetMapping("/{educationId}")
    @Transactional(readOnly = true)
    public ResponseEntity<EducationResponse> getEducationById(
            @PathVariable Long profileId,
            @PathVariable Long educationId) {
        log.info("GET /api/profiles/{}/education/{}", profileId, educationId);

        EducationResponse education = educationService.getEducationById(profileId, educationId);
        return ResponseEntity.ok(education);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<EducationResponse> createEducation(
            @PathVariable Long profileId,
            @Valid @RequestBody CreateEducationRequest request) {
        log.info("POST /api/profiles/{}/education", profileId);

        EducationResponse education = educationService.createEducation(profileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(education);
    }

    @PutMapping("/{educationId}")
    @Transactional
    public ResponseEntity<EducationResponse> updateEducation(
            @PathVariable Long profileId,
            @PathVariable Long educationId,
            @Valid @RequestBody UpdateEducationRequest request) {
        log.info("PUT /api/profiles/{}/education/{}", profileId, educationId);

        EducationResponse education = educationService.updateEducation(profileId, educationId, request);
        return ResponseEntity.ok(education);
    }

    @DeleteMapping("/{educationId}")
    @Transactional
    public ResponseEntity<Void> deleteEducation(
            @PathVariable Long profileId,
            @PathVariable Long educationId) {
        log.info("DELETE /api/profiles/{}/education/{}", profileId, educationId);

        educationService.deleteEducation(profileId, educationId);
        return ResponseEntity.noContent().build();
    }
}
