package com.diagnostic.mammogram.security;

import com.diagnostic.mammogram.model.User;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("userSecurity")
public class UserSecurity {
    public boolean isSelf(Authentication authentication, Long userId) {
        User principal = (User) authentication.getPrincipal();
        return principal.getId().equals(userId);
    }
}