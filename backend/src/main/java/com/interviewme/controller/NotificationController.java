package com.interviewme.controller;

import com.interviewme.dto.notification.NotificationDto;
import com.interviewme.model.User;
import com.interviewme.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @Transactional(readOnly = true)
    public ResponseEntity<List<NotificationDto>> list(
            Authentication authentication,
            @RequestParam(defaultValue = "false") boolean unreadOnly) {
        User user = (User) authentication.getPrincipal();
        List<NotificationDto> notifications = unreadOnly
            ? notificationService.listUnreadForUser(user.getId())
            : notificationService.listForUser(user.getId());
        return ResponseEntity.ok(notifications);
    }

    @GetMapping("/count")
    @Transactional(readOnly = true)
    public ResponseEntity<Map<String, Long>> countUnread(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        long count = notificationService.countUnread(user.getId());
        return ResponseEntity.ok(Map.of("unread", count));
    }

    @PostMapping("/{id}/read")
    @Transactional
    public ResponseEntity<Void> markAsRead(
            Authentication authentication,
            @PathVariable Long id) {
        User user = (User) authentication.getPrincipal();
        notificationService.markAsRead(id, user.getId());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    @Transactional
    public ResponseEntity<Void> markAllAsRead(Authentication authentication) {
        User user = (User) authentication.getPrincipal();
        notificationService.markAllAsRead(user.getId());
        return ResponseEntity.ok().build();
    }
}
