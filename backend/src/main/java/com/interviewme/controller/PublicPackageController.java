package com.interviewme.controller;

import com.interviewme.dto.packages.PublicPackageResponse;
import com.interviewme.service.PackageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/packages")
@RequiredArgsConstructor
@Slf4j
public class PublicPackageController {

    private final PackageService packageService;

    @GetMapping("/{slug}")
    @Transactional
    public ResponseEntity<PublicPackageResponse> getPublicPackage(
            @PathVariable String slug,
            @RequestParam(required = false) String t) {
        log.info("GET /api/public/packages/{}", slug);
        PublicPackageResponse response = packageService.getPublicPackage(slug, t);
        return ResponseEntity.ok(response);
    }
}
