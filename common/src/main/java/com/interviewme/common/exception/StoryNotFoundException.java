package com.interviewme.common.exception;

public class StoryNotFoundException extends RuntimeException {
    public StoryNotFoundException(Long storyId) {
        super("Story not found with id: " + storyId);
    }

    public StoryNotFoundException(String message) {
        super(message);
    }
}
