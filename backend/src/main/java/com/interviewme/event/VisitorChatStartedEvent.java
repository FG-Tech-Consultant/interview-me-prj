package com.interviewme.event;

import java.time.Instant;

public record VisitorChatStartedEvent(
        Long tenantId,
        Long profileId,
        Long visitorId,
        Long visitorSessionId,
        String visitorName,
        String visitorCompany,
        String visitorJobRole,
        String locale,
        Instant timestamp
) {}
