package com.diagnostic.mammogram.exception;

public class AIServiceException extends RuntimeException {
    // Constructor with message
    public AIServiceException(String message) {
        super(message);
    }

    // Constructor with message and cause
    public AIServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}