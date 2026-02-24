package com.interviewme.exports.dto;

public record ExportTemplateResponse(
    Long id,
    String name,
    String type,
    String description
) {}
