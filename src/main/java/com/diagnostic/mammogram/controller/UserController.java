package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.dto.request.UpdateRoleRequest;
import com.diagnostic.mammogram.dto.response.UserResponse; // Ensure this is imported
import com.diagnostic.mammogram.exception.EmailExistsException; // NEW: Import EmailExistsException
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

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    /**
     * Registers a new user in the system.
     * Accessible by anyone (no @PreAuthorize, handled by Spring Security config if public endpoint).
     *
     * @param request The registration request details.
     * @return ResponseEntity indicating success or failure of registration.
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest request) {
        try {
            // UserService.createUserFromRequest already handles exceptions and returns User entity
            User user = userService.createUserFromRequest(request);

            Map<String, Object> response = new HashMap<>();
            response.put("status", "success");
            response.put("message", "User registered successfully");
            response.put("userId", user.getId()); // Return ID of the created user

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (UsernameExistsException ex) {
            log.warn("Registration failed - username already exists: {}", ex.getUsername());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "conflict");
            errorResponse.put("message", ex.getMessage()); // Use message from exception
            errorResponse.put("suggestions", generateUsernameSuggestions(ex.getUsername()));

            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (EmailExistsException ex) { // Handle EmailExistsException
            // FIX: Use ex.getMessage() as EmailExistsException does not have getEmail()
            log.warn("Registration failed - email already exists: {}", ex.getMessage());
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "conflict");
            errorResponse.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT).body(errorResponse);

        } catch (UserCreationException ex) {
            log.error("User creation failed: {}", ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of(
                            "status", "error",
                            "message", ex.getMessage()
                    ));
        }
    }

    /**
     * Generates a list of username suggestions based on a conflicting username.
     * @param username The username that caused a conflict.
     * @return A list of suggested usernames.
     */
    private List<String> generateUsernameSuggestions(String username) {
        return List.of(
                username + "123",
                username + "_" + (int)(Math.random() * 1000),
                "dr_" + username,
                username.substring(0, Math.min(5, username.length())) + "_user"
        );
    }

    /**
     * Retrieves a user by their ID.
     * Accessible by ADMIN or the user themselves.
     *
     * @param userId The ID of the user to retrieve.
     * @return ResponseEntity with user details.
     */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN') or @userSecurity.isSelf(authentication, #userId)")
    public ResponseEntity<Map<String, Object>> getUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());

        try {
            log.debug("Fetching user details for ID: {}", userId);

            // UserService.getUserById now returns UserResponse directly
            UserResponse userResponse = userService.getUserById(userId);

            // Successful response
            response.put("success", true);
            response.put("message", "User details retrieved successfully");
            response.put("data", userResponse);

            log.info("Successfully retrieved user details for ID: {}", userId);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            log.warn("User not found with ID {}: {}", userId, ex.getMessage());
            response.put("success", false);
            response.put("message", ex.getMessage()); // Use message from exception
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (AccessDeniedException ex) {
            log.warn("Access denied for user ID {}: {}", userId, ex.getMessage());
            response.put("success", false);
            response.put("message", "Access denied");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception ex) {
            log.error("Unexpected error fetching user ID {}: {}", userId, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves a user by their username.
     * Accessible by ADMIN.
     *
     * @param username The username to search for.
     * @return ResponseEntity with user details.
     */
    @GetMapping("/username/{username}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUserByUsername(
            @PathVariable String username) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("searchedUsername", username);

        try {
            log.debug("Admin searching for user: {}", username);

            String normalizedUsername = username.trim().toLowerCase();

            // UserService.getUserByUsername now returns UserResponse directly
            UserResponse user = userService.getUserByUsername(normalizedUsername);

            response.put("success", true);
            response.put("message", "User found");
            response.put("data", user);

            log.info("Admin successfully retrieved user: {}", normalizedUsername);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            log.warn("User not found: {}", username);
            response.put("success", false);
            response.put("message", ex.getMessage()); // Use message from exception
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception ex) {
            log.error("Error searching for user {}: {}", username, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Error processing request");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Updates the role of a user.
     * Accessible by ADMIN.
     *
     * @param userId The ID of the user to update.
     * @param request The request containing the new role.
     * @return ResponseEntity with the updated user details.
     */
    @PutMapping("/{userId}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateRoleRequest request) { // Add @Valid for DTO validation

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());

        try {
            log.info("Updating role for user ID: {} to role: {}", userId, request.getRole());

            // UserService.updateUserRole now returns UserResponse directly
            UserResponse userResponse = userService.updateUserRole(userId, request.getRole());

            response.put("success", true);
            response.put("message", "User role updated successfully");
            response.put("data", userResponse);

            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            log.warn("User not found with ID {}: {}", userId, ex.getMessage());
            response.put("success", false);
            response.put("message", ex.getMessage()); // Use message from exception
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalArgumentException ex) {
            log.warn("Invalid role assignment for user ID {}: {}", userId, ex.getMessage());
            response.put("success", false);
            response.put("message", ex.getMessage()); // Use message from exception
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception ex) {
            log.error("Unexpected error updating role for user ID {}: {}", userId, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Internal server error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Retrieves a list of users filtered by role.
     * Accessible by ADMIN.
     *
     * @param roleInput The role string to filter by.
     * @return ResponseEntity with a list of users.
     */
    @GetMapping("/role/{role}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getUsersByRole(
            @PathVariable("role") String roleInput) {

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("requestedRole", roleInput);

        try {
            User.Role role = validateRole(roleInput);
            log.debug("Fetching users with role: {}", role);

            // UserService.getUsersByRole now returns List<UserResponse> directly
            List<UserResponse> users = userService.getUsersByRole(role);

            return buildSuccessResponse(role, users);

        } catch (IllegalArgumentException ex) {
            return handleInvalidRole(response, roleInput, ex);
        } catch (Exception ex) {
            return handleUnexpectedError(response, roleInput, ex);
        }
    }

    // Helper Methods for getUsersByRole
    private User.Role validateRole(String roleInput) {
        try {
            return User.Role.fromString(roleInput);
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid role conversion attempt: {}", roleInput);
            throw ex;
        }
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
        response.put("message", ex.getMessage()); // Use message from exception
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

    /**
     * Retrieves a list of all active doctors.
     * Accessible by ADMIN.
     *
     * @return ResponseEntity with a list of active doctors.
     */
    @GetMapping("/doctors/active")
    @PreAuthorize("hasRole('ADMIN')") // Added PreAuthorize for consistency
    public ResponseEntity<Map<String, Object>> getActiveDoctors() {
        Map<String, Object> response = new HashMap<>();
        try {
            log.debug("Fetching all active doctors");
            // UserService.getActiveDoctors now returns List<UserResponse> directly
            List<UserResponse> doctors = userService.getActiveDoctors();

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

    /**
     * Deactivates a user account.
     * Accessible by ADMIN.
     *
     * @param userId The ID of the user to deactivate.
     * @return ResponseEntity indicating success or failure of deactivation.
     */
    @PatchMapping("/{userId}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());

        try {
            log.info("Attempting to deactivate user ID: {}", userId);

            // Fetch user as UserResponse to check status, then call service
            UserResponse userResponse = userService.getUserById(userId);

            // Check if already deactivated
            if (!userResponse.isEnabled()) {
                response.put("success", false);
                response.put("message", "User is already deactivated");
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Proceed with deactivation
            // userService.deactivateUser returns void, but updateUserEnabledStatus returns UserResponse
            UserResponse updatedUser = userService.updateUserEnabledStatus(userId, false);

            response.put("success", true);
            response.put("message", "User deactivated successfully");
            response.put("userId", userId);
            response.put("data", updatedUser); // Include updated user data

            log.info("Successfully deactivated user ID: {}", userId);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            log.warn("Deactivation failed - user not found: ID {}", userId);
            response.put("success", false);
            response.put("message", ex.getMessage()); // Use message from exception
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception ex) {
            log.error("Unexpected error deactivating user ID {}: {}", userId, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Failed to deactivate user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Activates a user account.
     * Accessible by ADMIN.
     *
     * @param userId The ID of the user to activate.
     * @return ResponseEntity indicating success or failure of activation.
     */
    @PatchMapping("/{userId}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> activateUser(@PathVariable Long userId) {
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", System.currentTimeMillis());
        response.put("userId", userId);

        try {
            log.info("Admin attempting to activate user ID: {}", userId);

            // Fetch user as UserResponse to check status, then call service
            UserResponse userResponse = userService.getUserById(userId);

            // Check if already active
            if (userResponse.isEnabled()) {
                response.put("success", false);
                response.put("message", "User is already active");
                log.warn("Activation failed - user {} already active", userId);
                return ResponseEntity.status(HttpStatus.CONFLICT).body(response);
            }

            // Proceed with activation
            // userService.activateUser returns void, but updateUserEnabledStatus returns UserResponse
            UserResponse updatedUser = userService.updateUserEnabledStatus(userId, true);

            response.put("success", true);
            response.put("message", "User activated successfully");
            response.put("newStatus", true);
            response.put("data", updatedUser); // Include updated user data

            log.info("Admin successfully activated user ID: {}", userId);
            return ResponseEntity.ok(response);

        } catch (UserNotFoundException ex) {
            log.warn("Activation failed - user not found: ID {}", userId);
            response.put("success", false);
            response.put("message", ex.getMessage()); // Use message from exception
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception ex) {
            log.error("Unexpected error activating user ID {}: {}", userId, ex.getMessage(), ex);
            response.put("success", false);
            response.put("message", "Failed to activate user");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    /**
     * Searches for users based on a query string (e.g., username).
     * Accessible by ADMIN.
     *
     * @param query The search query.
     * @return ResponseEntity with a list of matching users.
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> searchUsers(@RequestParam String query) {
        Map<String, Object> response = new HashMap<>();
        try {
            log.debug("Searching users with query: {}", query);
            // UserService.searchUsers now returns List<UserResponse> directly
            List<UserResponse> users = userService.searchUsers(query);

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

    /**
     * Counts the number of admin users.
     * Accessible by ADMIN.
     *
     * @return ResponseEntity with the count of admin users.
     */
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

    /**
     * NEW: Retrieves a list of all users in the system.
     * Accessible by ADMIN.
     *
     * @return ResponseEntity with a list of all UserResponse DTOs.
     */
    @GetMapping // This endpoint will now correctly return all users
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllUsers() {
        log.info("Received request to get all users.");
        List<UserResponse> users = userService.getAllUsers(); // Call the service method that returns DTOs

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "All users retrieved successfully.");
        responseMap.put("data", users);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }
}
