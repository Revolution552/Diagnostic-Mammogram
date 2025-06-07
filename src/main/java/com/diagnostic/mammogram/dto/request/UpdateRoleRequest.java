package com.diagnostic.mammogram.dto.request;

import com.diagnostic.mammogram.model.User;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    private User.Role role;
}