package com.interviewme.mapper;

import com.interviewme.dto.linkedin.DraftPageResponse;
import com.interviewme.dto.linkedin.DraftResponse;
import com.interviewme.model.LinkedInDraft;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.stream.Collectors;

public class LinkedInDraftMapper {

    public static DraftResponse toResponse(LinkedInDraft entity) {
        return new DraftResponse(
            entity.getId(),
            entity.getOriginalMessage(),
            entity.getCategory().name(),
            entity.getSuggestedReply(),
            entity.getTone(),
            entity.getStatus().name(),
            entity.getCreatedAt(),
            entity.getUpdatedAt()
        );
    }

    public static DraftPageResponse toPageResponse(Page<LinkedInDraft> page) {
        List<DraftResponse> content = page.getContent().stream()
            .map(LinkedInDraftMapper::toResponse)
            .collect(Collectors.toList());

        return new DraftPageResponse(
            content,
            page.getNumber(),
            page.getSize(),
            page.getTotalElements(),
            page.getTotalPages()
        );
    }
}
