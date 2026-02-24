package com.interviewme.dto.publicprofile;

import java.util.List;
import java.util.Map;

public record PublicStoryResponse(
    String title,
    String situation,
    String task,
    String action,
    String result,
    Map<String, Object> metrics,
    List<String> linkedSkills
) {
}
