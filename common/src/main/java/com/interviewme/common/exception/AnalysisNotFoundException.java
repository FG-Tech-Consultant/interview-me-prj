package com.interviewme.common.exception;

public class AnalysisNotFoundException extends RuntimeException {
    public AnalysisNotFoundException(Long analysisId) {
        super("Analysis not found with id: " + analysisId);
    }

    public AnalysisNotFoundException(String message) {
        super(message);
    }
}
