package com.interviewme.controller;

import com.interviewme.dto.experience.CreateStoryRequest;
import com.interviewme.dto.experience.StoryResponse;
import com.interviewme.dto.experience.UpdateStoryRequest;
import com.interviewme.service.StoryService;
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
public class StoryController {

    private final StoryService storyService;

    @GetMapping("/api/v1/projects/{projectId}/stories")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StoryResponse>> getStoriesByProject(@PathVariable Long projectId) {
        log.info("GET /api/v1/projects/{}/stories", projectId);
        List<StoryResponse> stories = storyService.getStoriesByProject(projectId);
        return ResponseEntity.ok(stories);
    }

    @PostMapping("/api/v1/projects/{projectId}/stories")
    @Transactional
    public ResponseEntity<StoryResponse> createStory(
            @PathVariable Long projectId,
            @Valid @RequestBody CreateStoryRequest request) {
        log.info("POST /api/v1/projects/{}/stories", projectId);
        StoryResponse story = storyService.createStory(projectId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(story);
    }

    @GetMapping("/api/v1/stories/{storyId}")
    @Transactional(readOnly = true)
    public ResponseEntity<StoryResponse> getStory(@PathVariable Long storyId) {
        log.info("GET /api/v1/stories/{}", storyId);
        StoryResponse story = storyService.getStoryById(storyId);
        return ResponseEntity.ok(story);
    }

    @PutMapping("/api/v1/stories/{storyId}")
    @Transactional
    public ResponseEntity<StoryResponse> updateStory(
            @PathVariable Long storyId,
            @Valid @RequestBody UpdateStoryRequest request) {
        log.info("PUT /api/v1/stories/{}", storyId);
        StoryResponse story = storyService.updateStory(storyId, request);
        return ResponseEntity.ok(story);
    }

    @DeleteMapping("/api/v1/stories/{storyId}")
    @Transactional
    public ResponseEntity<Void> deleteStory(@PathVariable Long storyId) {
        log.info("DELETE /api/v1/stories/{}", storyId);
        storyService.deleteStory(storyId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/v1/profiles/{profileId}/stories/public")
    @Transactional(readOnly = true)
    public ResponseEntity<List<StoryResponse>> getPublicStories(@PathVariable Long profileId) {
        log.info("GET /api/v1/profiles/{}/stories/public", profileId);
        List<StoryResponse> stories = storyService.getPublicStoriesByProfile(profileId);
        return ResponseEntity.ok(stories);
    }
}
