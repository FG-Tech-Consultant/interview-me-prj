package com.interviewme.common.exception;

public class SectionNotApplicableException extends RuntimeException {
    public SectionNotApplicableException(String sectionName) {
        super("Suggestions for '" + sectionName + "' section cannot be directly applied to profile");
    }
}
