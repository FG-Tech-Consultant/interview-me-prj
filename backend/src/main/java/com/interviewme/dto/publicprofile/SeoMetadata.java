package com.interviewme.dto.publicprofile;

import java.util.List;

public record SeoMetadata(
    String title,
    String description,
    String canonicalUrl,
    List<String> keywords
) {
}
