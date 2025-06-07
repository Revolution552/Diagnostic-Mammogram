package com.diagnostic.mammogram.dto.request;

import com.diagnostic.mammogram.model.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 20, message = "Username must be between 3 and 20 characters")
    private String username;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 40, message = "Password must be between 6 and 40 characters")
    private String password;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|DOCTOR|RADIOLOGIST|PATIENT",
            flags = Pattern.Flag.CASE_INSENSITIVE,
            message = "Invalid role. Must be ADMIN, DOCTOR, RADIOLOGIST or PATIENT")
    private String role;

    // Add method to convert to enum
    public Role getRoleEnum() {
        return Role.valueOf(role.toUpperCase());
    }
}