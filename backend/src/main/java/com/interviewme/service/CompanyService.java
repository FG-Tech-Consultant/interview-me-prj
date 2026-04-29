package com.interviewme.service;

import com.interviewme.dto.company.*;
import com.interviewme.mapper.CompanyMapper;
import com.interviewme.model.Company;
import com.interviewme.model.Tenant;
import com.interviewme.model.User;
import com.interviewme.repository.CompanyRepository;
import com.interviewme.repository.TenantRepository;
import com.interviewme.repository.UserRepository;
import com.interviewme.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CompanyService {

    private final CompanyRepository companyRepository;
    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public CompanyRegistrationResponse registerCompany(CompanyRegistrationRequest request) {
        log.info("Registering company: {}", request.companyName());

        // Check if email already exists
        if (userRepository.findByEmail(request.email()).isPresent()) {
            throw new IllegalArgumentException("Email already registered");
        }

        // 1. Create Tenant
        Tenant tenant = new Tenant();
        tenant.setName(request.companyName());
        tenant = tenantRepository.save(tenant);

        // 2. Create User with COMPANY_ADMIN role
        User user = new User();
        user.setTenantId(tenant.getId());
        user.setEmail(request.email());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole("COMPANY_ADMIN");
        user = userRepository.save(user);

        // 3. Create Company
        Company company = new Company();
        company.setTenantId(tenant.getId());
        company.setName(request.companyName());
        company.setSector(request.sector());
        company.setSize(request.size());
        company.setWebsite(request.website());
        company.setCountry(request.country());
        company.setCity(request.city());
        company.setDescription(request.description());
        company = companyRepository.save(company);

        // 4. Generate JWT token
        String token = jwtService.generateToken(user.getEmail(), tenant.getId());

        log.info("Company registered successfully: id={}, tenantId={}", company.getId(), tenant.getId());
        return new CompanyRegistrationResponse(company.getId(), token, "Company registered successfully");
    }

    @Transactional(readOnly = true)
    public CompanyProfileDto getProfile(Long tenantId) {
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found for tenant: " + tenantId));
        return CompanyMapper.toProfileDto(company);
    }

    @Transactional
    public CompanyProfileDto updateProfile(Long tenantId, CompanyUpdateRequest request) {
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found for tenant: " + tenantId));
        CompanyMapper.updateEntity(company, request);
        company = companyRepository.save(company);
        log.info("Company profile updated: id={}", company.getId());
        return CompanyMapper.toProfileDto(company);
    }

    @Transactional(readOnly = true)
    public CompanyDashboardDto getDashboardMetrics(Long tenantId) {
        Company company = companyRepository.findByTenantId(tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found for tenant: " + tenantId));
        // Placeholder metrics - will be populated when job postings are implemented
        return new CompanyDashboardDto(
            company.getName(),
            0L,
            0L,
            0L
        );
    }
}
