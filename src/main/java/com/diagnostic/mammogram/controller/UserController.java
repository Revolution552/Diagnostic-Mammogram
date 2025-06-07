package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.dto.request.UpdateRoleRequest;
import com.diagnostic.mammogram.dto.response.UserResponse;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse createUser(@RequestBody RegisterRequest request) {
        User user = userService.createUserFromRequest(request);
        return toUserResponse(user);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(authentication, #userId)")
    public UserResponse getUser(@PathVariable Long userId) {
        return toUserResponse(userService.getUserById(userId));
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse getUserByUsername(@PathVariable String username) {
        return toUserResponse(userService.getUserByUsername(username));
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUserRole(
            @PathVariable Long userId,
            @RequestBody UpdateRoleRequest request) {
        return toUserResponse(userService.updateUserRole(userId, request.getRole()));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> getUsersByRole(@PathVariable User.Role role) {
        return userService.getUsersByRole(role).stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/doctors/active")
    public List<UserResponse> getActiveDoctors() {
        return userService.getActiveDoctors().stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @PatchMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public void deactivateUser(@PathVariable Long userId) {
        userService.deactivateUser(userId);
    }

    @PatchMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public void activateUser(@PathVariable Long userId) {
        userService.activateUser(userId);
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public List<UserResponse> searchUsers(@RequestParam String query) {
        return userService.searchUsers(query).stream()
                .map(this::toUserResponse)
                .collect(Collectors.toList());
    }

    @GetMapping("/count/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public long countAdmins() {
        return userService.countAdmins();
    }

    // Helper method to convert User to UserResponse DTO
    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
}