package com.diagnostic.mammogram.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthenticationResponse {
    private String token;
    private String username;
    private String role;

    // No need for custom builder implementation
    // Lombok will generate AuthenticationResponseBuilder automatically
}