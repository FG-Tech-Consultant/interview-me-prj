package com.interviewme.dto.skill;

import java.util.List;

public record SkillDto(
    Long id,
    String name,
    String category,
    String description,
    List<String> tags,
    Boolean isActive
) {
}
