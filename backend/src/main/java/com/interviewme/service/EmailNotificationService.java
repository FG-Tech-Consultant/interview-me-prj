package com.interviewme.service;

import com.interviewme.config.NotificationProperties;
import com.interviewme.model.Profile;
import com.interviewme.model.User;
import com.interviewme.repository.ProfileRepository;
import com.interviewme.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailNotificationService {

    private final JavaMailSender mailSender;
    private final ProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final NotificationProperties notificationProperties;

    public void sendVisitorChatNotification(Long profileId, String visitorName, String visitorCompany, String visitorJobRole) {
        if (!notificationProperties.isEnabled()) {
            log.debug("Email notifications disabled, skipping for profileId={}", profileId);
            return;
        }

        Profile profile = profileRepository.findById(profileId).orElse(null);
        if (profile == null) {
            log.warn("Profile not found for notification: profileId={}", profileId);
            return;
        }

        User user = userRepository.findById(profile.getUserId()).orElse(null);
        if (user == null) {
            log.warn("User not found for notification: userId={}", profile.getUserId());
            return;
        }

        String recipientEmail = user.getEmail();
        String visitorsUrl = notificationProperties.getBaseUrl() + "/visitors";
        String firstName = profile.getFullName().split(" ")[0];

        String subject = "Someone interviewed your profile on Interview Me!";
        String body = """
                Hi %s,

                You had a visitor on your Interview Me profile!

                Visitor: %s
                Company: %s
                Role: %s

                They asked questions about your experience through your public profile chat.

                View your visitors: %s

                --
                Interview Me - Live Resume & Career Copilot
                """.formatted(firstName,
                visitorName != null ? visitorName : "Anonymous",
                visitorCompany != null ? visitorCompany : "Not provided",
                visitorJobRole != null ? visitorJobRole : "Not provided",
                visitorsUrl);

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(notificationProperties.getFrom());
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Visitor chat notification sent to={} for profileId={} visitor={} company={}",
                    recipientEmail, profileId, visitorName, visitorCompany);
        } catch (Exception e) {
            log.error("Failed to send visitor chat notification to={}: {}", recipientEmail, e.getMessage());
        }
    }
}
