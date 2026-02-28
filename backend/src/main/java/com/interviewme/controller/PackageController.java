package com.interviewme.controller;

import com.interviewme.dto.packages.*;
import com.interviewme.service.PackageService;
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

import java.util.List;

@RestController
@RequestMapping("/api/packages")
@RequiredArgsConstructor
@Slf4j
public class PackageController {

    private final PackageService packageService;
    private final ProfileService profileService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<PackageResponse>> listPackages(
            @AuthenticationPrincipal UserDetails userDetails) {
        log.info("GET /api/packages");
        Long profileId = getProfileId(userDetails);
        List<PackageResponse> packages = packageService.getPackagesByProfile(profileId);
        return ResponseEntity.ok(packages);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public ResponseEntity<PackageDetailResponse> getPackage(@PathVariable Long id) {
        log.info("GET /api/packages/{}", id);
        PackageDetailResponse detail = packageService.getPackageDetail(id);
        return ResponseEntity.ok(detail);
    }

    @PostMapping
    @Transactional
    public ResponseEntity<PackageResponse> createPackage(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CreatePackageRequest request) {
        log.info("POST /api/packages");
        Long profileId = getProfileId(userDetails);
        PackageResponse response = packageService.createPackage(profileId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<PackageResponse> updatePackage(
            @PathVariable Long id,
            @Valid @RequestBody UpdatePackageRequest request) {
        log.info("PUT /api/packages/{}", id);
        PackageResponse response = packageService.updatePackage(id, request);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    @Transactional
    public ResponseEntity<Void> deletePackage(@PathVariable Long id) {
        log.info("DELETE /api/packages/{}", id);
        packageService.deletePackage(id);
        return ResponseEntity.noContent().build();
    }

    // --- Skill items ---

    @PostMapping("/{id}/skills")
    @Transactional
    public ResponseEntity<Void> addSkill(
            @PathVariable Long id,
            @Valid @RequestBody AddPackageItemRequest request) {
        log.info("POST /api/packages/{}/skills", id);
        packageService.addSkill(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    @Transactional
    public ResponseEntity<Void> removeSkill(
            @PathVariable Long id,
            @PathVariable Long skillId) {
        log.info("DELETE /api/packages/{}/skills/{}", id, skillId);
        packageService.removeSkill(id, skillId);
        return ResponseEntity.noContent().build();
    }

    // --- Project items ---

    @PostMapping("/{id}/projects")
    @Transactional
    public ResponseEntity<Void> addProject(
            @PathVariable Long id,
            @Valid @RequestBody AddPackageItemRequest request) {
        log.info("POST /api/packages/{}/projects", id);
        packageService.addProject(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/projects/{projectId}")
    @Transactional
    public ResponseEntity<Void> removeProject(
            @PathVariable Long id,
            @PathVariable Long projectId) {
        log.info("DELETE /api/packages/{}/projects/{}", id, projectId);
        packageService.removeProject(id, projectId);
        return ResponseEntity.noContent().build();
    }

    // --- Story items ---

    @PostMapping("/{id}/stories")
    @Transactional
    public ResponseEntity<Void> addStory(
            @PathVariable Long id,
            @Valid @RequestBody AddPackageItemRequest request) {
        log.info("POST /api/packages/{}/stories", id);
        packageService.addStory(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{id}/stories/{storyId}")
    @Transactional
    public ResponseEntity<Void> removeStory(
            @PathVariable Long id,
            @PathVariable Long storyId) {
        log.info("DELETE /api/packages/{}/stories/{}", id, storyId);
        packageService.removeStory(id, storyId);
        return ResponseEntity.noContent().build();
    }

    // --- Token regeneration ---

    @PostMapping("/{id}/regenerate-token")
    @Transactional
    public ResponseEntity<PackageResponse> regenerateToken(@PathVariable Long id) {
        log.info("POST /api/packages/{}/regenerate-token", id);
        PackageResponse response = packageService.regenerateToken(id);
        return ResponseEntity.ok(response);
    }

    private Long getProfileId(UserDetails userDetails) {
        Long userId = Long.parseLong(userDetails.getUsername());
        return profileService.getProfileByUserId(userId).id();
    }
}
