package com.interviewme.controller;

import com.interviewme.dto.experience.CreateProjectRequest;
import com.interviewme.dto.experience.ProjectResponse;
import com.interviewme.dto.experience.UpdateProjectRequest;
import com.interviewme.service.ExperienceProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@Slf4j
public class ExperienceProjectController {

    private final ExperienceProjectService projectService;

    @GetMapping("/api/v1/jobs/{jobId}/projects")
    @Transactional(readOnly = true)
    public ResponseEntity<List<ProjectResponse>> getProjectsByJob(@PathVariable Long jobId) {
        log.info("GET /api/v1/jobs/{}/projects", jobId);
        List<ProjectResponse> projects = projectService.getProjectsByJobExperience(jobId);
        return ResponseEntity.ok(projects);
    }

    @PostMapping("/api/v1/jobs/{jobId}/projects")
    @Transactional
    public ResponseEntity<ProjectResponse> createProject(
            @PathVariable Long jobId,
            @Valid @RequestBody CreateProjectRequest request) {
        log.info("POST /api/v1/jobs/{}/projects", jobId);
        ProjectResponse project = projectService.createProject(jobId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(project);
    }

    @GetMapping("/api/v1/projects/{projectId}")
    @Transactional(readOnly = true)
    public ResponseEntity<ProjectResponse> getProject(@PathVariable Long projectId) {
        log.info("GET /api/v1/projects/{}", projectId);
        ProjectResponse project = projectService.getProjectById(projectId);
        return ResponseEntity.ok(project);
    }

    @PutMapping("/api/v1/projects/{projectId}")
    @Transactional
    public ResponseEntity<ProjectResponse> updateProject(
            @PathVariable Long projectId,
            @Valid @RequestBody UpdateProjectRequest request) {
        log.info("PUT /api/v1/projects/{}", projectId);
        ProjectResponse project = projectService.updateProject(projectId, request);
        return ResponseEntity.ok(project);
    }

    @DeleteMapping("/api/v1/projects/{projectId}")
    @Transactional
    public ResponseEntity<Void> deleteProject(@PathVariable Long projectId) {
        log.info("DELETE /api/v1/projects/{}", projectId);
        projectService.deleteProject(projectId);
        return ResponseEntity.noContent().build();
    }
}
