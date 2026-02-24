package com.interviewme.common.exception;

public class ProjectNotFoundException extends RuntimeException {
    public ProjectNotFoundException(Long projectId) {
        super("Project not found with id: " + projectId);
    }

    public ProjectNotFoundException(String message) {
        super(message);
    }
}
