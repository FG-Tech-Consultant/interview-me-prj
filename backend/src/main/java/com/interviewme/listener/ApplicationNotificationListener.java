package com.interviewme.listener;

import com.interviewme.event.ApplicationStatusChangedEvent;
import com.interviewme.event.ApplicationSubmittedEvent;
import com.interviewme.model.NotificationType;
import com.interviewme.model.User;
import com.interviewme.repository.UserRepository;
import com.interviewme.service.EmailNotificationService;
import com.interviewme.service.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class ApplicationNotificationListener {

    private final NotificationService notificationService;
    private final EmailNotificationService emailNotificationService;
    private final UserRepository userRepository;

    private static final int HIGH_FIT_THRESHOLD = 80;

    @Async
    @EventListener
    public void onApplicationSubmitted(ApplicationSubmittedEvent event) {
        log.info("Application submitted: applicationId={} job={} candidate={}",
            event.applicationId(), event.jobTitle(), event.candidateName());

        // Notify company admins (users in the same tenant with COMPANY_ADMIN role)
        List<User> admins = userRepository.findByTenantId(event.tenantId());
        for (User admin : admins) {
            if (admin.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_COMPANY_ADMIN"))) {

                // New application notification
                notificationService.create(
                    event.tenantId(), admin.getId(),
                    NotificationType.NEW_APPLICATION,
                    "New Application: " + event.jobTitle(),
                    event.candidateName() + " applied for " + event.jobTitle(),
                    "job_application", event.applicationId()
                );

                // High fit notification
                if (event.fitScore() != null && event.fitScore() >= HIGH_FIT_THRESHOLD) {
                    notificationService.create(
                        event.tenantId(), admin.getId(),
                        NotificationType.HIGH_FIT_CANDIDATE,
                        "High Fit Candidate! (" + event.fitScore() + "%)",
                        event.candidateName() + " scored " + event.fitScore()
                            + "% fit for " + event.jobTitle(),
                        "job_application", event.applicationId()
                    );
                }
            }
        }
    }

    @Async
    @EventListener
    public void onApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        log.info("Application status changed: applicationId={} {} -> {}",
            event.applicationId(), event.oldStatus(), event.newStatus());

        // Notify the candidate (if they have a user account)
        if (event.userId() != null) {
            notificationService.create(
                event.tenantId(), event.userId(),
                NotificationType.APPLICATION_STATUS_CHANGED,
                "Application Update: " + event.jobTitle(),
                "Your application for " + event.jobTitle()
                    + " has been updated to: " + event.newStatus(),
                "job_application", event.applicationId()
            );
        }

        // Send email to candidate
        emailNotificationService.sendApplicationStatusEmail(
            event.candidateEmail(), event.jobTitle(), event.newStatus());
    }
}
