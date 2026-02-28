package com.interviewme.controller;

import com.interviewme.service.AccountDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/internal/test")
@RequiredArgsConstructor
@Slf4j
public class TestCleanupController {

    private final AccountDeletionService accountDeletionService;

    @Value("${test.cleanup.api-key:}")
    private String testApiKey;

    @DeleteMapping("/cleanup")
    @Transactional
    public ResponseEntity<Map<String, Object>> cleanupTestAccounts(
            @RequestHeader("X-Test-Api-Key") String apiKey,
            @RequestBody Map<String, String> body) {

        if (testApiKey.isEmpty() || !testApiKey.equals(apiKey)) {
            return ResponseEntity.status(403).body(Map.of(
                "error", "FORBIDDEN",
                "message", "Invalid test API key."
            ));
        }

        String emailPattern = body.get("emailPattern");
        if (emailPattern == null || emailPattern.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "BAD_REQUEST",
                "message", "emailPattern is required."
            ));
        }

        log.info("Cleaning up test accounts matching pattern: {}", emailPattern);
        int deletedCount = accountDeletionService.deleteAccountsByEmailPattern(emailPattern);

        return ResponseEntity.ok(Map.of(
            "deletedAccounts", deletedCount,
            "message", "Test accounts matching pattern have been deleted."
        ));
    }
}
