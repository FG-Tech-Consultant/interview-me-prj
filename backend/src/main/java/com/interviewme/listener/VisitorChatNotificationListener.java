package com.interviewme.listener;

import com.interviewme.config.NotificationProperties;
import com.interviewme.event.VisitorChatStartedEvent;
import com.interviewme.service.EmailNotificationService;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.util.concurrent.*;

@Component
@Slf4j
public class VisitorChatNotificationListener {

    private final EmailNotificationService emailNotificationService;
    private final NotificationProperties notificationProperties;
    private final ScheduledExecutorService scheduler;
    private final ConcurrentHashMap<Long, ScheduledFuture<?>> scheduledNotifications;

    public VisitorChatNotificationListener(
            EmailNotificationService emailNotificationService,
            NotificationProperties notificationProperties) {
        this.emailNotificationService = emailNotificationService;
        this.notificationProperties = notificationProperties;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(
                Thread.ofVirtual().name("notification-scheduler-", 0).factory());
        this.scheduledNotifications = new ConcurrentHashMap<>();
    }

    @EventListener
    public void onVisitorChatStarted(VisitorChatStartedEvent event) {
        if (!notificationProperties.isEnabled()) {
            return;
        }

        Long sessionId = event.visitorSessionId();

        if (scheduledNotifications.containsKey(sessionId)) {
            log.debug("Notification already scheduled for visitorSessionId={}", sessionId);
            return;
        }

        long delayMinutes = notificationProperties.getDelayMinutes();
        log.info("Scheduling visitor chat notification: visitorSessionId={} profileId={} visitor={} delay={}min",
                sessionId, event.profileId(), event.visitorName(), delayMinutes);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            try {
                emailNotificationService.sendVisitorChatNotification(
                        event.profileId(),
                        event.visitorName(),
                        event.visitorCompany(),
                        event.visitorJobRole());
            } finally {
                scheduledNotifications.remove(sessionId);
            }
        }, delayMinutes, TimeUnit.MINUTES);

        scheduledNotifications.put(sessionId, future);
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
        log.info("Notification scheduler shut down. {} pending notifications lost.", scheduledNotifications.size());
    }
}
