package com.interviewme.dto.publicprofile;

import java.util.List;

public record SlugCheckResponse(
    String slug,
    boolean available,
    List<String> suggestions
) {
}
