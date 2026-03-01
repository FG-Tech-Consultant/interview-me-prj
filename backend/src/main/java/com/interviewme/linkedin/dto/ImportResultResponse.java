package com.interviewme.linkedin.dto;

import com.interviewme.model.ImportStatus;

import java.util.List;
import java.util.Map;

public record ImportResultResponse(
    Long importId,
    ImportStatus status,
    Map<String, Integer> importedCounts,
    List<String> errors
) {}
