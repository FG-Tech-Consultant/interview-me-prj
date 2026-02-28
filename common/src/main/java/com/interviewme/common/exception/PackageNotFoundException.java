package com.interviewme.common.exception;

public class PackageNotFoundException extends RuntimeException {
    public PackageNotFoundException(Long packageId) {
        super("Package not found with id: " + packageId);
    }

    public PackageNotFoundException(String message) {
        super(message);
    }
}
