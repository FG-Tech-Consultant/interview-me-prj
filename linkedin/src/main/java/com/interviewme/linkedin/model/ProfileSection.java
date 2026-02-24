package com.interviewme.linkedin.model;

public enum ProfileSection {
    HEADLINE,
    ABOUT,
    EXPERIENCE,
    EDUCATION,
    SKILLS,
    RECOMMENDATIONS,
    OTHER;

    public boolean canApplyToProfile() {
        return this == HEADLINE || this == ABOUT;
    }
}
