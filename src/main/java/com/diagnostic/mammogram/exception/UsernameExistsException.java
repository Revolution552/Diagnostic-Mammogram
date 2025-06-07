package com.diagnostic.mammogram.exception;


import lombok.Getter;

@Getter
public class UsernameExistsException extends RuntimeException {
    // 'this' keyword for clarity (optional)
    private final String username;

    public UsernameExistsException(String username) {
        super("Username '" + username + "' already exists");
        this.username = username;
    }

}