package com.interviewme.dto.visitor;

import java.time.Instant;

public record VisitorSessionResponse(
    Long id,
    Long visitorId,
    String visitorName,
    String visitorCompany,
    Instant startedAt,
    Instant endedAt,
    int messageCount
) {}
