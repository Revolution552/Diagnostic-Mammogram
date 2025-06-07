package com.diagnostic.mammogram.dto.request;

import com.diagnostic.mammogram.model.User;
import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    // Constant definitions
    private static final String ROLE_PATTERN = "^(?i)(ADMIN|DOCTOR|RADIOLOGIST|PATIENT)$";
    private static final String ALLOWED_ROLES = "ADMIN, DOCTOR, RADIOLOGIST, PATIENT";

    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "Username can only contain letters, numbers and underscores")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 40, message = "Password must be between 8 and 40 characters")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=!])(?=\\S+$).{8,}$",
            message = "Password must contain at least one digit, one lowercase, one uppercase, and one special character")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = ROLE_PATTERN,
            message = "Invalid role. Allowed values: " + ALLOWED_ROLES)
    private String role;

    /**
     * Converts the string role to User.Role enum with validation
     */
    public User.Role getRoleAsEnum() {
        try {
            return User.Role.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                    "Invalid role '" + role + "'. Allowed values: " + ALLOWED_ROLES);
        }
    }
}