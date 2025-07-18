package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.dto.response.UserResponse; // NEW: Import UserResponse DTO
import com.diagnostic.mammogram.exception.*;
import com.diagnostic.mammogram.model.PasswordResetToken;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.model.VerificationToken;
import com.diagnostic.mammogram.repository.PasswordResetTokenRepository;
import com.diagnostic.mammogram.repository.UserRepository;
import com.diagnostic.mammogram.repository.VerificationTokenRepository;
import com.diagnostic.mammogram.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors; // NEW: Import Collectors for stream operations


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional // Class-level transactional, fine for most operations here
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService; // Injected but not used in provided methods
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Creates a new user in the system.
     *
     * @param request The registration request containing user details.
     * @return The created User entity.
     * @throws UsernameExistsException if the username already exists.
     */
    public User createUser(RegisterRequest request) {
        log.info("Attempting to create user with username: {}", request.getUsername());
        // Check username availability first
        if (userRepository.existsByUsername(request.getUsername())) {
            log.warn("User creation failed: Username '{}' already exists.", request.getUsername());
            throw new UsernameExistsException(request.getUsername());
        }

        // Check email availability (optional, but good practice for unique emails)
        if (userRepository.findByEmailIgnoreCase(request.getEmail()).isPresent()) {
            log.warn("User creation failed: Email '{}' already exists.", request.getEmail());
            throw new EmailExistsException(request.getEmail()); // Assuming you have an EmailExistsException
        }

        // Create and save new user
        User user = User.builder()
                .fullName(request.getFullName())
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(User.Role.fromString(request.getRole())) // Use fromString to safely convert role string
                .enabled(true) // Default to enabled
                .build();

        User savedUser = userRepository.save(user);
        log.info("User '{}' created successfully with ID: {}", savedUser.getUsername(), savedUser.getId());
        return savedUser;
    }

    /**
     * Creates a user from a registration request, providing more specific exception handling.
     *
     * @param request The registration request.
     * @return The created User entity.
     * @throws UsernameExistsException if the username already exists.
     * @throws EmailExistsException if the email already exists.
     * @throws UserCreationException if any other error occurs during user creation.
     */
    public User createUserFromRequest(RegisterRequest request) {
        try {
            return createUser(request);
        } catch (UsernameExistsException | EmailExistsException ex) {
            // Log the specific conflict and re-throw
            log.warn("Registration failed: {}", ex.getMessage());
            throw ex;
        } catch (Exception ex) {
            log.error("User creation failed for {}: {}", request.getUsername(), ex.getMessage(), ex);
            throw new UserCreationException("Failed to create user: " + ex.getMessage());
        }
    }

    // --- Private Helper Methods ---
    private VerificationToken createVerificationToken(User user) {
        VerificationToken token = new VerificationToken();
        token.setUser(user);
        token.setToken(generateSixDigitCode());
        token.generateTimestamps(1); // 1 hour expiry
        return verificationTokenRepository.save(token);
    }

    private PasswordResetToken createPasswordResetToken(User user) {
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setToken(generateSixDigitCode());
        token.generateTimestamps(30); // 30 minutes expiry
        return passwordResetTokenRepository.save(token);
    }

    private String generateSixDigitCode() {
        return String.format("%06d", new Random().nextInt(900000) + 100000);
    }


    /**
     * Initiates the password reset process by sending a reset email.
     *
     * @param email The email of the user requesting a password reset.
     * @throws EmailNotFoundException if no user is found with the given email.
     * @throws EmailFailureException if there's an issue sending the email.
     */
    public void forgotPassword(String email) throws EmailNotFoundException, EmailFailureException {
        Optional<User> opUser = userRepository.findByEmailIgnoreCase(email);
        if (opUser.isPresent()) {
            User user = opUser.get();

            PasswordResetToken token = createPasswordResetToken(user);

            try {
                emailService.sendPasswordResetEmail(user, token.getToken());
                logger.info("Password reset email sent successfully to: {}", email);
            } catch (EmailFailureException e) {
                logger.error("Failed to send password reset email to: {}", email, e);
                throw e;
            }
        } else {
            logger.warn("Email not found for password reset: {}", email);
            throw new EmailNotFoundException("No user found with email: " + email);
        }
    }

    /**
     * Resets a user's password using a valid reset token.
     *
     * @param token The password reset token.
     * @param newPassword The new password.
     * @param confirmPassword The confirmation password.
     * @return true if password reset was successful.
     * @throws IllegalArgumentException if passwords don't match, token is invalid, or token is expired.
     */
    public boolean resetPasswordWithToken(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            logger.warn("Password reset failed - passwords do not match for token: {}", token);
            throw new IllegalArgumentException("New password and confirmation password do not match.");
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            logger.warn("Password reset failed - invalid token: {}", token);
            throw new IllegalArgumentException("Invalid or expired token.");
        }

        PasswordResetToken resetToken = tokenOpt.get();

        if (resetToken.isExpired()) {
            logger.warn("Password reset failed - token expired: {}", token);
            throw new IllegalArgumentException("Reset token has expired.");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        logger.info("Password reset successfully for token: {}", token);
        return true;
    }

    /**
     * Retrieves a user by their ID.
     *
     * @param userId The ID of the user.
     * @return The UserResponse DTO.
     * @throws UserNotFoundException if the user is not found.
     */
    @Transactional(readOnly = true) // Mark as read-only
    public UserResponse getUserById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        return mapToUserResponse(user);
    }

    /**
     * NEW: Retrieves all users from the system.
     *
     * @return A list of UserResponse DTOs.
     */
    @Transactional(readOnly = true) // Mark as read-only
    public List<UserResponse> getAllUsers() {
        log.info("Fetching all users.");
        return userRepository.findAll().stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Updates the role of a specific user.
     *
     * @param userId The ID of the user to update.
     * @param newRole The new role to assign.
     * @return The updated UserResponse DTO.
     * @throws UserNotFoundException if the user is not found.
     */
    public UserResponse updateUserRole(Long userId, User.Role newRole) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
        user.setRole(newRole);
        User updatedUser = userRepository.save(user);
        log.info("User ID {} role updated to {}.", userId, newRole);
        return mapToUserResponse(updatedUser);
    }

    /**
     * NEW: Updates the enabled status of a user.
     * This method can activate or deactivate a user.
     *
     * @param userId The ID of the user to update.
     * @param enabled The new enabled status (true for active, false for deactivated).
     * @return The updated UserResponse DTO.
     * @throws UserNotFoundException if the user is not found.
     */
    public UserResponse updateUserEnabledStatus(Long userId, boolean enabled) {
        log.info("Updating enabled status for user ID: {} to {}", userId, enabled);
        // Using the custom repository method for direct update
        int updatedRows = userRepository.updateUserStatus(userId, enabled);
        if (updatedRows == 0) {
            throw new UserNotFoundException("User not found with ID: " + userId);
        }
        // Fetch the updated user to return the DTO
        return getUserById(userId); // Reuse existing method to fetch and map to DTO
    }

    /**
     * Deactivates a user account.
     *
     * @param userId The ID of the user to deactivate.
     * @throws UserNotFoundException if the user is not found.
     */
    // This method can be removed if updateUserEnabledStatus is preferred, or kept for semantic clarity.
    // If kept, it should call updateUserEnabledStatus(userId, false).
    public void deactivateUser(Long userId) {
        log.info("Deactivating user ID: {}", userId);
        updateUserEnabledStatus(userId, false);
    }

    /**
     * Activates a user account.
     *
     * @param userId The ID of the user to activate.
     * @throws UserNotFoundException if the user is not found.
     */
    // This method can be removed if updateUserEnabledStatus is preferred, or kept for semantic clarity.
    // If kept, it should call updateUserEnabledStatus(userId, true).
    public void activateUser(Long userId) {
        log.info("Activating user ID: {}", userId);
        updateUserEnabledStatus(userId, true);
    }

    /**
     * Retrieves a list of users by their role.
     *
     * @param role The role to filter by.
     * @return A list of UserResponse DTOs.
     */
    @Transactional(readOnly = true) // Mark as read-only
    public List<UserResponse> getUsersByRole(User.Role role) {
        log.info("Fetching users by role: {}", role);
        return userRepository.findByRole(role).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retrieves a user by their username.
     *
     * @param username The username to search for.
     * @return The UserResponse DTO.
     * @throws UserNotFoundException if the user is not found.
     */
    @Transactional(readOnly = true) // Mark as read-only
    public UserResponse getUserByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
        return mapToUserResponse(user);
    }

    /**
     * Retrieves a list of all active doctors.
     *
     * @return A list of UserResponse DTOs for active doctors.
     */
    @Transactional(readOnly = true) // Mark as read-only
    public List<UserResponse> getActiveDoctors() {
        log.info("Fetching all active doctors.");
        return userRepository.findByRoleAndEnabled(User.Role.DOCTOR, true).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Counts the number of users with the ADMIN role.
     *
     * @return The count of admin users.
     */
    @Transactional(readOnly = true) // Mark as read-only
    public long countAdmins() {
        log.info("Counting admin users.");
        return userRepository.countByRole(User.Role.ADMIN);
    }

    /**
     * Searches for users whose username matches the given query.
     *
     * @param query The search query string.
     * @return A list of UserResponse DTOs matching the search criteria.
     */
    @Transactional(readOnly = true) // Mark as read-only
    public List<UserResponse> searchUsers(String query) {
        log.info("Searching users with query: {}", query);
        return userRepository.searchByUsername(query).stream()
                .map(this::mapToUserResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to map User entity to UserResponse DTO.
     * @param user The User entity.
     * @return The corresponding UserResponse DTO.
     */
    private UserResponse mapToUserResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .build();
    }
}
