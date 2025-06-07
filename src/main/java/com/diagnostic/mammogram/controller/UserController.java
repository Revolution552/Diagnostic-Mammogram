package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.dto.request.UpdateRoleRequest;
import com.diagnostic.mammogram.dto.response.UserResponse;
import com.diagnostic.mammogram.exception.UserCreationException;
import com.diagnostic.mammogram.exception.UsernameExistsException;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            User user = userService.createUserFromRequest(request);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User registered successfully");
            response.put("userId", user.getId());

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UsernameExistsException ex) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "conflict");
            errorResponse.put("message", "Username already exists");
            errorResponse.put("suggestions", generateUsernameSuggestions(ex.getUsername()));

            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (UserCreationException ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", ex.getMessage()
                    ));
        }
    }

    private List<String> generateUsernameSuggestions(String username) {
        return List.of(
                username + "123",
                username + "_" + (int)(Math.random() * 1000),
                "dr_" + username,
                username.substring(0, Math.min(5, username.length())) + "_user"
        );
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(authentication, #userId)")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.debug("Fetching user details for ID: {}", userId);
            UserResponse userResponse = toUserResponse(userService.getUserById(userId));

            response.put("success", true);
            response.put("message", "User details retrieved successfully");
            response.put("data", userResponse);
            response.put("timestamp", System.currentTimeMillis());

            log.info("Successfully retrieved user details for ID: {}", userId);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Error fetching user with ID {}: {}", userId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve user: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserByUsername(@PathVariable String username) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.debug("Fetching user by username: {}", username);
            UserResponse userResponse = toUserResponse(userService.getUserByUsername(username));

            response.put("success", true);
            response.put("message", "User details retrieved successfully");
            response.put("data", userResponse);
            response.put("timestamp", System.currentTimeMillis());

            log.info("Successfully retrieved user by username: {}", username);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Error fetching user with username {}: {}", username, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve user: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        }
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody UpdateRoleRequest request) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Updating role for user ID: {} to role: {}", userId, request.getRole());
            UserResponse userResponse = toUserResponse(userService.updateUserRole(userId, request.getRole()));

            response.put("success", true);
            response.put("message", "User role updated successfully");
            response.put("data", userResponse);
            response.put("timestamp", System.currentTimeMillis());

            log.info("Successfully updated role for user ID: {} to {}", userId, request.getRole());
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Error updating role for user ID {}: {}", userId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to update user role: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsersByRole(@PathVariable User.Role role) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.debug("Fetching all users with role: {}", role);
            List<UserResponse> users = userService.getUsersByRole(role).stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("message", String.format("Retrieved %d users with role %s", users.size(), role));
            response.put("data", users);
            response.put("count", users.size());
            response.put("timestamp", System.currentTimeMillis());

            log.info("Successfully retrieved {} users with role: {}", users.size(), role);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Error fetching users with role {}: {}", role, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve users: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/doctors/active")
    public ResponseEntity<Map<String, Object>> getActiveDoctors() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.debug("Fetching all active doctors");
            List<UserResponse> doctors = userService.getActiveDoctors().stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("message", String.format("Retrieved %d active doctors", doctors.size()));
            response.put("data", doctors);
            response.put("count", doctors.size());
            response.put("timestamp", System.currentTimeMillis());

            log.info("Successfully retrieved {} active doctors", doctors.size());
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Error fetching active doctors: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to retrieve active doctors: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PatchMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Deactivating user with ID: {}", userId);
            userService.deactivateUser(userId);

            response.put("success", true);
            response.put("message", "User deactivated successfully");
            response.put("userId", userId);
            response.put("timestamp", System.currentTimeMillis());

            log.info("Successfully deactivated user with ID: {}", userId);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Error deactivating user ID {}: {}", userId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to deactivate user: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PatchMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.info("Activating user with ID: {}", userId);
            userService.activateUser(userId);

            response.put("success", true);
            response.put("message", "User activated successfully");
            response.put("userId", userId);
            response.put("timestamp", System.currentTimeMillis());

            log.info("Successfully activated user with ID: {}", userId);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Error activating user ID {}: {}", userId, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to activate user: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.debug("Searching users with query: {}", query);
            List<UserResponse> users = userService.searchUsers(query).stream()
                    .map(this::toUserResponse)
                    .collect(Collectors.toList());

            response.put("success", true);
            response.put("message", String.format("Found %d users matching query", users.size()));
            response.put("data", users);
            response.put("count", users.size());
            response.put("query", query);
            response.put("timestamp", System.currentTimeMillis());

            log.info("Found {} users matching query: {}", users.size(), query);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Error searching users with query {}: {}", query, e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Search failed: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/count/admins")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> countAdmins() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.debug("Counting admin users");
            long count = userService.countAdmins();

            response.put("success", true);
            response.put("message", String.format("Found %d admin users", count));
            response.put("count", count);
            response.put("timestamp", System.currentTimeMillis());

            log.info("Found {} admin users", count);
            return ResponseEntity.ok().body(response);
        } catch (Exception e) {
            log.error("Error counting admin users: {}", e.getMessage(), e);
            response.put("success", false);
            response.put("message", "Failed to count admin users: " + e.getMessage());
            response.put("timestamp", System.currentTimeMillis());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private UserResponse toUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
}