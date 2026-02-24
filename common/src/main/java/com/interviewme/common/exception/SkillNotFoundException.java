package com.interviewme.common.exception;

public class SkillNotFoundException extends RuntimeException {
    public SkillNotFoundException(Long skillId) {
        super("Skill not found with id: " + skillId);
    }

    public SkillNotFoundException(String message) {
        super(message);
    }
}
