package com.diagnostic.mammogram.dto.response;

import com.diagnostic.mammogram.model.User;
import jakarta.persistence.Column;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private User.Role role;
    private boolean enabled;
}