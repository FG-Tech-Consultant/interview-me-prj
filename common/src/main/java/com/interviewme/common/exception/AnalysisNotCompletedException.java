package com.interviewme.common.exception;

public class AnalysisNotCompletedException extends RuntimeException {
    public AnalysisNotCompletedException(Long analysisId) {
        super("Analysis " + analysisId + " is not completed yet");
    }
}
