package com.interviewme.controller;

import com.interviewme.model.User;
import com.interviewme.service.AccountDeletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/account")
@RequiredArgsConstructor
@Slf4j
public class AccountController {

    private final AccountDeletionService accountDeletionService;

    @DeleteMapping
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteAccount(
            @RequestBody Map<String, String> body,
            Authentication authentication) {

        String confirmation = body.get("confirmation");
        if (!"DELETE MY ACCOUNT".equals(confirmation)) {
            return ResponseEntity.badRequest().body(Map.of(
                "error", "CONFIRMATION_REQUIRED",
                "message", "Provide confirmation text 'DELETE MY ACCOUNT' to proceed."
            ));
        }

        User user = (User) authentication.getPrincipal();
        Map<String, Integer> deletedCounts = accountDeletionService.deleteAccount(
            user.getTenantId(), user.getId()
        );

        return ResponseEntity.ok(Map.of(
            "message", "Account and all associated data have been permanently deleted.",
            "deletedCounts", deletedCounts
        ));
    }
}
