package com.interviewme.controller;

import com.interviewme.dto.company.*;
import com.interviewme.model.User;
import com.interviewme.service.CompanyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/company")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping("/register")
    @Transactional
    public ResponseEntity<CompanyRegistrationResponse> register(
            @Valid @RequestBody CompanyRegistrationRequest request) {
        CompanyRegistrationResponse response = companyService.registerCompany(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/profile")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<CompanyProfileDto> getProfile(Authentication authentication) {
        Long tenantId = ((User) authentication.getPrincipal()).getTenantId();
        CompanyProfileDto profile = companyService.getProfile(tenantId);
        return ResponseEntity.ok(profile);
    }

    @PutMapping("/profile")
    @Transactional
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<CompanyProfileDto> updateProfile(
            Authentication authentication,
            @Valid @RequestBody CompanyUpdateRequest request) {
        Long tenantId = ((User) authentication.getPrincipal()).getTenantId();
        CompanyProfileDto profile = companyService.updateProfile(tenantId, request);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    @PreAuthorize("hasRole('COMPANY_ADMIN')")
    public ResponseEntity<CompanyDashboardDto> getDashboard(Authentication authentication) {
        Long tenantId = ((User) authentication.getPrincipal()).getTenantId();
        CompanyDashboardDto dashboard = companyService.getDashboardMetrics(tenantId);
        return ResponseEntity.ok(dashboard);
    }
}
