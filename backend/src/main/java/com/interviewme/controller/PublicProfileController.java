package com.interviewme.controller;

import com.interviewme.dto.publicprofile.PublicProfileResponse;
import com.interviewme.service.PublicProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/profiles")
@RequiredArgsConstructor
@Slf4j
public class PublicProfileController {

    private final PublicProfileService publicProfileService;

    @GetMapping("/{slug}")
    @Transactional(readOnly = true)
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable String slug) {
        log.info("GET /api/public/profiles/{}", slug);
        PublicProfileResponse response = publicProfileService.getPublicProfile(slug);
        return ResponseEntity.ok(response);
    }
}
