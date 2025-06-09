package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.dto.request.UpdateRoleRequest;
import com.diagnostic.mammogram.dto.response.UserResponse;
import com.diagnostic.mammogram.exception.UserCreationException;
import com.diagnostic.mammogram.exception.UserNotFoundException;
import com.diagnostic.mammogram.exception.UsernameExistsException;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
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
        response.put("timestamp", System.currentTimeMillis());

        try {
            log.debug("Fetching user details for ID: {}", userId);

            // Fetch user (will throw UserNotFoundException if not found)
            UserResponse userResponse = toUserResponse(userService.getUserById(userId));

            // Successful response
            response.put("success", true);
            response.put("message", "User details retrieved successfully");
            response.put("data", userResponse);

            log.info("Successfully retrieved user details for ID: {}", userId);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            // Specific handling for "not found" cases
            log.warn("User not found with ID {}: {}", userId, ex.getMessage());
            response.put("success", false);
            response.put("message", "User not found with ID: " + userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (AccessDeniedException ex) {
            // Handle authorization failures separately
            log.warn("Access denied for user ID {}: {}", userId, ex.getMessage());
            response.put("success", false);
            response.put("message", "Access denied");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception ex) {
            // Catch-all for other unexpected errors
            log.error("Unexpected error fetching user ID {}: {}", userId, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserByUsername(
            @PathVariable String username) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("searchedUsername", username);

        try {
            log.debug("Admin searching for user: {}", username);

            // Trim and case-normalize the username
            String normalizedUsername = username.trim().toLowerCase();

            // Fetch user (throws UserNotFoundException if not found)
            UserResponse user = toUserResponse(
                    userService.getUserByUsername(normalizedUsername)
            );

            response.put("success", true);
            response.put("message", "User found");
            response.put("data", user);

            log.info("Admin successfully retrieved user: {}", normalizedUsername);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            log.warn("User not found: {}", username);
            response.put("success", false);
            response.put("message", "User not found with username: " + username);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception ex) {
            log.error("Error searching for user {}: {}", username, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Error processing request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long userId,
            @RequestBody UpdateRoleRequest request) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());

        try {
            log.info("Updating role for user ID: {} to role: {}", userId, request.getRole());

            // This will throw UserNotFoundException if user doesn't exist
            UserResponse userResponse = toUserResponse(userService.updateUserRole(userId, request.getRole()));

            response.put("success", true);
            response.put("message", "User role updated successfully");
            response.put("data", userResponse);

            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            log.warn("User not found with ID {}: {}", userId, ex.getMessage());
            response.put("success", false);
            response.put("message", "User not found with ID: " + userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid role assignment for user ID {}: {}", userId, ex.getMessage());
            response.put("success", false);
            response.put("message", "Invalid role: " + request.getRole());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception ex) {
            log.error("Unexpected error updating role for user ID {}: {}", userId, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsersByRole(
            @PathVariable("role") String roleInput) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("requestedRole", roleInput);

        try {
            // 1. Validate and convert role
            User.Role role = validateRole(roleInput);
            log.debug("Fetching users with role: {}", role);

            // 2. Fetch users
            List<UserResponse> users = fetchUsersByRole(role);

            // 3. Build success response
            return buildSuccessResponse(role, users);

        } catch (IllegalArgumentException ex) {
            return handleInvalidRole(response, roleInput, ex);
        } catch (Exception ex) {
            return handleUnexpectedError(response, roleInput, ex);
        }
    }

    // Helper Methods
    private User.Role validateRole(String roleInput) {
        try {
            return User.Role.fromString(roleInput);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid role conversion attempt: {}", roleInput);
            throw ex;
        }
    }

    private List<UserResponse> fetchUsersByRole(User.Role role) {
        return userService.getUsersByRole(role)
                .stream()
                .map(this::toUserResponse)
                .toList();
    }

    private ResponseEntity<Map<String, Object>> buildSuccessResponse(
            User.Role role, List<UserResponse> users) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("success", true);
        response.put("count", users.size());
        response.put("data", users);
        response.put("normalizedRole", role.name());

        log.info("Fetched {} users with role {}", users.size(), role);
        return ResponseEntity.ok(response);
    }

    private ResponseEntity<Map<String, Object>> handleInvalidRole(
            Map<String, Object> response, String roleInput, IllegalArgumentException ex) {
        response.put("success", false);
        response.put("message", "Invalid role specified: " + roleInput);
        response.put("validRoles", Arrays.stream(User.Role.values())
                .map(Enum::name)
                .toList());

        log.warn("Invalid role request: {}", roleInput);
        return ResponseEntity.badRequest().body(response);
    }

    private ResponseEntity<Map<String, Object>> handleUnexpectedError(
            Map<String, Object> response, String roleInput, Exception ex) {
        response.put("success", false);
        response.put("message", "Failed to process request");

        log.error("Error fetching users with role {}: {}", roleInput, ex.getMessage(), ex);
        return ResponseEntity.internalServerError().body(response);
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
        response.put("timestamp", System.currentTimeMillis());

        try {
            log.info("Attempting to deactivate user ID: {}", userId);

            // First verify user exists
            User user = userService.getUserById(userId);

            // Check if already deactivated
            if (!user.isEnabled()) {
                response.put("success", false);
                response.put("message", "User is already deactivated");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Proceed with deactivation
            userService.deactivateUser(userId);

            response.put("success", true);
            response.put("message", "User deactivated successfully");
            response.put("userId", userId);

            log.info("Successfully deactivated user ID: {}", userId);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            log.warn("Deactivation failed - user not found: ID {}", userId);
            response.put("success", false);
            response.put("message", "User not found with ID: " + userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception ex) {
            log.error("Unexpected error deactivating user ID {}: {}", userId, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Failed to deactivate user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PatchMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("userId", userId);

        try {
            log.info("Admin attempting to activate user ID: {}", userId);

            // First verify user exists
            User user = userService.getUserById(userId);

            // Check if already active
            if (user.isEnabled()) {
                response.put("success", false);
                response.put("message", "User is already active");
                log.warn("Activation failed - user {} already active", userId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Proceed with activation
            userService.activateUser(userId);

            response.put("success", true);
            response.put("message", "User activated successfully");
            response.put("newStatus", true);

            log.info("Admin successfully activated user ID: {}", userId);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            log.warn("Activation failed - user not found: ID {}", userId);
            response.put("success", false);
            response.put("message", "User not found with ID: " + userId);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception ex) {
            log.error("Unexpected error activating user ID {}: {}", userId, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Failed to activate user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
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