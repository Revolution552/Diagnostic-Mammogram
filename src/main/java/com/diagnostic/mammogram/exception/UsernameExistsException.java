package com.diagnostic.mammogram.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // Returns 409 Conflict status
public class UsernameExistsException extends RuntimeException {
    public UsernameExistsException(String username) {
        super(String.format("Username '%s' already exists", username));
    }

    public UsernameExistsException(String username, Throwable cause) {
        super(String.format("Username '%s' already exists", username), cause);
    }
}