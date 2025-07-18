package com.diagnostic.mammogram.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409 Conflict
public class EmailExistsException extends RuntimeException {
    public EmailExistsException(String email) {
        super("Email '" + email + "' already exists.");
    }
}