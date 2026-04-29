package com.interviewme.service;

import com.interviewme.dto.notification.NotificationDto;
import com.interviewme.model.Notification;
import com.interviewme.model.NotificationType;
import com.interviewme.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Transactional
    public Notification create(Long tenantId, Long userId, NotificationType type,
                               String title, String message,
                               String referenceType, Long referenceId) {
        Notification n = new Notification();
        n.setTenantId(tenantId);
        n.setUserId(userId);
        n.setType(type);
        n.setTitle(title);
        n.setMessage(message);
        n.setReferenceType(referenceType);
        n.setReferenceId(referenceId);

        n = notificationRepository.save(n);
        log.info("Notification created: id={} type={} userId={}", n.getId(), type, userId);
        return n;
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId)
            .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> listUnreadForUser(Long userId) {
        return notificationRepository.findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId)
            .stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByUserIdAndReadAtIsNull(userId);
    }

    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId)
            .filter(n -> n.getUserId().equals(userId) && n.getReadAt() == null)
            .ifPresent(n -> {
                n.setReadAt(OffsetDateTime.now());
                notificationRepository.save(n);
                log.debug("Notification marked as read: id={}", notificationId);
            });
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> unread = notificationRepository
            .findByUserIdAndReadAtIsNullOrderByCreatedAtDesc(userId);
        OffsetDateTime now = OffsetDateTime.now();
        for (Notification n : unread) {
            n.setReadAt(now);
        }
        notificationRepository.saveAll(unread);
        log.info("Marked {} notifications as read for userId={}", unread.size(), userId);
    }

    private NotificationDto toDto(Notification n) {
        return new NotificationDto(
            n.getId(), n.getType().name(), n.getTitle(), n.getMessage(),
            n.getReferenceType(), n.getReferenceId(),
            n.getReadAt() != null, n.getCreatedAt()
        );
    }
}
