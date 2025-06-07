package com.diagnostic.mammogram.dto.response;

import com.diagnostic.mammogram.model.User;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {
    private Long id;
    private String username;
    private User.Role role;
    private boolean enabled;
}