package com.interviewme.common.exception;

public class PublicProfileNotFoundException extends RuntimeException {

    public PublicProfileNotFoundException(String slug) {
        super("No public profile found for slug: " + slug);
    }
}
