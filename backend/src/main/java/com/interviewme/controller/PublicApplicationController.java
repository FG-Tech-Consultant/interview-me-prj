package com.interviewme.controller;

import com.interviewme.dto.job.ApplicationChatRequest;
import com.interviewme.dto.job.ApplicationChatResponse;
import com.interviewme.dto.job.LinkedInUploadResponse;
import com.interviewme.service.ApplicationChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/jobs")
@RequiredArgsConstructor
@Slf4j
public class PublicApplicationController {

    private final ApplicationChatService applicationChatService;

    /**
     * POST /api/public/jobs/{slug}/apply/chat — AI-guided application chatbot (no auth)
     */
    @PostMapping("/{slug}/apply/chat")
    @Transactional
    public ResponseEntity<ApplicationChatResponse> applicationChat(
            @PathVariable String slug,
            @Valid @RequestBody ApplicationChatRequest request) {
        log.info("Application chat slug={} messageLength={}", slug,
            request.message() != null ? request.message().length() : 0);
        ApplicationChatResponse response = applicationChatService.chat(slug, request);
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/public/jobs/{slug}/apply/linkedin — upload LinkedIn PDF for auto-fill (no auth)
     */
    @PostMapping(value = "/{slug}/apply/linkedin", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Transactional
    public ResponseEntity<LinkedInUploadResponse> uploadLinkedIn(
            @PathVariable String slug,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "sessionToken", required = false) UUID sessionToken) {
        log.info("LinkedIn PDF upload slug={} fileSize={}", slug, file.getSize());

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                new LinkedInUploadResponse(false, sessionToken, null, null, null, null, "File is empty"));
        }

        if (!isPdfFile(file)) {
            return ResponseEntity.badRequest().body(
                new LinkedInUploadResponse(false, sessionToken, null, null, null, null,
                    "Only PDF files are accepted"));
        }

        LinkedInUploadResponse response = applicationChatService.uploadLinkedInPdf(slug, file, sessionToken);
        return ResponseEntity.ok(response);
    }

    private boolean isPdfFile(MultipartFile file) {
        String contentType = file.getContentType();
        String name = file.getOriginalFilename();
        return (contentType != null && contentType.equals("application/pdf")) ||
               (name != null && name.toLowerCase().endsWith(".pdf"));
    }
}
