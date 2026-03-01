package com.interviewme.linkedin.dto;

import com.interviewme.model.ImportStatus;
import com.interviewme.model.ImportStrategy;

import java.time.Instant;
import java.util.Map;

public record ImportHistoryResponse(
    Long id,
    String filename,
    ImportStrategy strategy,
    ImportStatus status,
    Map<String, Integer> itemCounts,
    Instant importedAt
) {}
