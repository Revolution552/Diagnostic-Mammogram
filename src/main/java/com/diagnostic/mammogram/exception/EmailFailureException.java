package com.diagnostic.mammogram.exception;

public class EmailFailureException extends RuntimeException {

    public EmailFailureException() {
        super("Failed to send email.");
    }

    public EmailFailureException(String message) {
        super(message);
    }

    public EmailFailureException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailFailureException(Throwable cause) {
        super(cause);
    }
}
