package com.interviewme.exports.dto;

import java.util.List;

public record ExportHistoryPageResponse(
    List<ExportHistoryResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
