package com.interviewme.service;

import com.interviewme.common.util.TenantContext;
import com.interviewme.dto.job.*;
import com.interviewme.model.JobPosting;
import com.interviewme.model.JobPostingStatus;
import com.interviewme.repository.JobApplicationRepository;
import com.interviewme.repository.JobPostingRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class JobPostingService {

    private final JobPostingRepository jobPostingRepository;
    private final JobApplicationRepository jobApplicationRepository;

    @Transactional
    public JobPostingDto createJobPosting(Long tenantId, Long companyId, CreateJobPostingRequest request) {
        String slug = generateSlug(request.title());

        JobPosting job = new JobPosting();
        job.setTenantId(tenantId);
        job.setCompanyId(companyId);
        job.setTitle(request.title());
        job.setSlug(slug);
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setBenefits(request.benefits());
        job.setLocation(request.location());
        job.setWorkModel(request.workModel());
        job.setSalaryRange(request.salaryRange());
        job.setExperienceLevel(request.experienceLevel());
        job.setStatus(JobPostingStatus.ACTIVE);
        job.setRequiredSkills(request.requiredSkills());
        job.setNiceToHaveSkills(request.niceToHaveSkills());

        job = jobPostingRepository.save(job);
        log.info("Created job posting id={} slug={} tenantId={}", job.getId(), slug, tenantId);

        return toDto(job);
    }

    @Transactional(readOnly = true)
    public List<JobPostingDto> listByCompany(Long companyId) {
        return jobPostingRepository.findByCompanyIdAndDeletedAtIsNullOrderByCreatedAtDesc(companyId)
            .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<JobPostingDto> listByTenant(Long tenantId) {
        return jobPostingRepository.findByTenantIdAndDeletedAtIsNullOrderByCreatedAtDesc(tenantId)
            .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public JobPostingDto getById(Long id, Long tenantId) {
        JobPosting job = jobPostingRepository.findById(id)
            .filter(j -> j.getTenantId().equals(tenantId) && j.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Job posting not found: " + id));
        return toDto(job);
    }

    @Transactional
    public JobPostingDto updateJobPosting(Long id, Long tenantId, UpdateJobPostingRequest request) {
        JobPosting job = jobPostingRepository.findById(id)
            .filter(j -> j.getTenantId().equals(tenantId) && j.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Job posting not found: " + id));

        if (request.title() != null) job.setTitle(request.title());
        if (request.description() != null) job.setDescription(request.description());
        if (request.requirements() != null) job.setRequirements(request.requirements());
        if (request.benefits() != null) job.setBenefits(request.benefits());
        if (request.location() != null) job.setLocation(request.location());
        if (request.workModel() != null) job.setWorkModel(request.workModel());
        if (request.salaryRange() != null) job.setSalaryRange(request.salaryRange());
        if (request.experienceLevel() != null) job.setExperienceLevel(request.experienceLevel());
        if (request.status() != null) job.setStatus(JobPostingStatus.valueOf(request.status()));
        if (request.requiredSkills() != null) job.setRequiredSkills(request.requiredSkills());
        if (request.niceToHaveSkills() != null) job.setNiceToHaveSkills(request.niceToHaveSkills());

        job = jobPostingRepository.save(job);
        log.info("Updated job posting id={} tenantId={}", id, tenantId);

        return toDto(job);
    }

    @Transactional
    public void deleteJobPosting(Long id, Long tenantId) {
        JobPosting job = jobPostingRepository.findById(id)
            .filter(j -> j.getTenantId().equals(tenantId) && j.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Job posting not found: " + id));

        job.setDeletedAt(OffsetDateTime.now());
        job.setStatus(JobPostingStatus.CLOSED);
        jobPostingRepository.save(job);
        log.info("Deleted (soft) job posting id={} tenantId={}", id, tenantId);
    }

    @Transactional(readOnly = true)
    public JobPostingDto getByIdWithStats(Long id, Long tenantId) {
        JobPosting job = jobPostingRepository.findById(id)
            .filter(j -> j.getTenantId().equals(tenantId) && j.getDeletedAt() == null)
            .orElseThrow(() -> new EntityNotFoundException("Job posting not found: " + id));

        long applicationCount = jobApplicationRepository.countByJobPostingIdAndDeletedAtIsNull(id);

        return new JobPostingDto(
            job.getId(), job.getTitle(), job.getSlug(), job.getDescription(),
            job.getRequirements(), job.getBenefits(), job.getLocation(),
            job.getWorkModel(), job.getSalaryRange(), job.getExperienceLevel(),
            job.getStatus().name(), job.getRequiredSkills(), job.getNiceToHaveSkills(),
            job.getCreatedAt()
        );
    }

    private String generateSlug(String title) {
        String base = title.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9\\s-]", "")
            .replaceAll("\\s+", "-")
            .replaceAll("-+", "-")
            .replaceAll("^-|-$", "");

        if (base.length() > 80) base = base.substring(0, 80);

        String slug = base;
        int attempt = 0;
        while (jobPostingRepository.existsBySlugAndDeletedAtIsNull(slug)) {
            attempt++;
            slug = base + "-" + UUID.randomUUID().toString().substring(0, 6);
        }
        return slug;
    }

    private JobPostingDto toDto(JobPosting job) {
        return new JobPostingDto(
            job.getId(), job.getTitle(), job.getSlug(), job.getDescription(),
            job.getRequirements(), job.getBenefits(), job.getLocation(),
            job.getWorkModel(), job.getSalaryRange(), job.getExperienceLevel(),
            job.getStatus().name(), job.getRequiredSkills(), job.getNiceToHaveSkills(),
            job.getCreatedAt()
        );
    }
}
