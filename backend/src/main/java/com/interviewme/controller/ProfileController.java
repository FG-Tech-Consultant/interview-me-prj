package com.interviewme.controller;

import com.interviewme.dto.profile.CreateProfileRequest;
import com.interviewme.dto.profile.ProfileResponse;
import com.interviewme.dto.profile.UpdateProfileRequest;
import com.interviewme.service.ProfileService;
import com.interviewme.dto.publicprofile.SlugCheckResponse;
import com.interviewme.service.PublicProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final ProfileService profileService;
    private final PublicProfileService publicProfileService;

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public ResponseEntity<ProfileResponse> getCurrentUserProfile(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        log.info("GET /api/profiles/me - userId: {}", userId);

        ProfileResponse profile = profileService.getProfileByUserId(userId);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/{profileId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProfileResponse> getProfileById(@PathVariable Long profileId) {
        log.info("GET /api/profiles/{}", profileId);

        ProfileResponse profile = profileService.getProfileById(profileId);
        return ResponseEntity.ok(profile);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ProfileResponse> createProfile(
            Authentication authentication,
            @Valid @RequestBody CreateProfileRequest request) {
        Long userId = Long.parseLong(authentication.getName());
        log.info("POST /api/profiles - userId: {}", userId);

        ProfileResponse profile = profileService.createProfile(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(profile);
    }

    @PutMapping("/{profileId}")
    @Transactional
    public ResponseEntity<ProfileResponse> updateProfile(
            @PathVariable Long profileId,
            @Valid @RequestBody UpdateProfileRequest request) {
        log.info("PUT /api/profiles/{}", profileId);

        ProfileResponse profile = profileService.updateProfile(profileId, request);
        return ResponseEntity.ok(profile);
    }

    @DeleteMapping("/{profileId}")
    @Transactional
    public ResponseEntity<Void> deleteProfile(@PathVariable Long profileId) {
        log.info("DELETE /api/profiles/{}", profileId);

        profileService.deleteProfile(profileId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/exists")
    @Transactional(readOnly = true)
    public ResponseEntity<Boolean> checkProfileExists(Authentication authentication) {
        Long userId = Long.parseLong(authentication.getName());
        log.info("GET /api/profiles/exists - userId: {}", userId);

        boolean exists = profileService.profileExists(userId);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/slug/check")
    @Transactional(readOnly = true)
    public ResponseEntity<SlugCheckResponse> checkSlugAvailability(@RequestParam String slug) {
        log.info("GET /api/profiles/slug/check - slug: {}", slug);
        SlugCheckResponse response = publicProfileService.checkSlugAvailability(slug);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{profileId}/slug")
    @Transactional
    public ResponseEntity<ProfileResponse> updateSlug(
            @PathVariable Long profileId,
            @RequestBody Map<String, String> body) {
        String slug = body.get("slug");
        log.info("PUT /api/profiles/{}/slug - slug: {}", profileId, slug);

        ProfileResponse profile = profileService.updateSlug(profileId, slug);
        return ResponseEntity.ok(profile);
    }
}
