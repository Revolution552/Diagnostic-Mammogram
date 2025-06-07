package com.diagnostic.mammogram.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND) // Returns 404 Not Found status
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super(String.format("User not found with ID: %d", userId));
    }

    public UserNotFoundException(String username) {
        super(String.format("User not found with username: %s", username));
    }

    public UserNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}