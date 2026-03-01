package com.interviewme.linkedin.dto;

import com.interviewme.model.ImportStrategy;

public record ConfirmImportRequest(String previewId, ImportStrategy importStrategy) {}
