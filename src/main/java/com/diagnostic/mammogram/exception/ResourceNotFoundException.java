package com.diagnostic.mammogram.exception;

public class ResourceNotFoundException extends RuntimeException {
    private String details;

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String message, String details) {
        super(message);
        this.details = details;
    }

    public String getDetails() {
        return details;
    }
}