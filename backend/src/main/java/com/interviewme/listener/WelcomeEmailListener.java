package com.interviewme.listener;

import com.interviewme.event.UserRegisteredEvent;
import com.interviewme.service.EmailNotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class WelcomeEmailListener {

    private final EmailNotificationService emailNotificationService;

    @Async
    @EventListener
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("User registered event: userId={} email={}", event.userId(), event.email());
        // Default to English since we don't know the user's locale at registration time
        emailNotificationService.sendWelcomeEmail(event.email(), event.tenantName(), "en");
    }
}
