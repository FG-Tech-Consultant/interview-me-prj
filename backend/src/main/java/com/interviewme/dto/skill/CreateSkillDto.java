package com.interviewme.dto.skill;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateSkillDto(
    @NotBlank(message = "Skill name is required")
    @Size(max = 255, message = "Skill name must not exceed 255 characters")
    String name,

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    String category,

    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    String description,

    @Size(max = 20, message = "Maximum 20 tags allowed")
    List<String> tags
) {
}
