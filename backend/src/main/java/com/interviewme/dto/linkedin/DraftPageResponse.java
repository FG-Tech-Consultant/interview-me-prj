package com.interviewme.dto.linkedin;

import java.util.List;

public record DraftPageResponse(
    List<DraftResponse> content,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
