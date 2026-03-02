package com.interviewme.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "notification.email")
public class NotificationProperties {
    private boolean enabled = false;
    private String from;
    private int delayMinutes = 60;
    private String baseUrl = "http://localhost:8080";
}
