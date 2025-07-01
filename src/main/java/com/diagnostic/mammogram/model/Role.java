package com.diagnostic.mammogram.model;

import org.springframework.security.core.GrantedAuthority;

import java.util.Arrays;

public enum Role implements GrantedAuthority {
    ADMIN,
    DOCTOR,
    RADIOLOGIST,
    PATIENT; // Assuming PATIENT role is also used

    @Override
    public String getAuthority() {
        return "ROLE_" + this.name();
    }

    // Helper method to convert string to Role enum
    public static Role fromString(String value) throws IllegalArgumentException {
        try {
            return Role.valueOf(value.toUpperCase());
        } catch (NullPointerException | IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    String.format("Invalid role '%s'. Valid roles are: %s",
                            value,
                            Arrays.toString(Role.values()))
            );
        }
    }
}
