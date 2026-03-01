package com.interviewme.linkedin.dto;

import com.interviewme.linkedin.dto.LinkedInProfileData;

import java.util.List;
import java.util.Map;

public record ImportPreviewResponse(
    String previewId,
    Map<String, Integer> counts,
    List<String> warnings,
    LinkedInProfileData profilePreview
) {}
