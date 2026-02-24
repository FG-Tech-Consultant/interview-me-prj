package com.interviewme.exports.mapper;

import com.interviewme.exports.dto.ExportHistoryResponse;
import com.interviewme.exports.dto.ExportStatusResponse;
import com.interviewme.exports.dto.ExportTemplateResponse;
import com.interviewme.exports.model.ExportHistory;
import com.interviewme.exports.model.ExportTemplate;

public final class ExportMapper {

    private ExportMapper() {}

    public static ExportTemplateResponse toResponse(ExportTemplate entity) {
        return new ExportTemplateResponse(
                entity.getId(),
                entity.getName(),
                entity.getType(),
                entity.getDescription()
        );
    }

    public static ExportHistoryResponse toResponse(ExportHistory entity) {
        return new ExportHistoryResponse(
                entity.getId(),
                entity.getTemplate() != null ? toResponse(entity.getTemplate()) : null,
                entity.getType(),
                entity.getStatus(),
                entity.getParameters(),
                entity.getCoinsSpent(),
                entity.getErrorMessage(),
                entity.getRetryCount(),
                entity.getCreatedAt(),
                entity.getCompletedAt()
        );
    }

    public static ExportStatusResponse toStatusResponse(ExportHistory entity) {
        return new ExportStatusResponse(
                entity.getId(),
                entity.getStatus(),
                entity.getErrorMessage(),
                entity.getRetryCount(),
                entity.getCompletedAt()
        );
    }
}
