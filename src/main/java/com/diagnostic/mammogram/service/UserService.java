package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.request.RegisterRequest;
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



@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final JwtService jwtService;
    private final VerificationTokenRepository verificationTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;


    public User createUser(RegisterRequest request) {
        log.info("Creating user: {}", request.getUsername()); // Now works
        // Check username availability first
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameExistsException(request.getUsername());
        }

        // Create and save new user
        User user = new User();
        user.setFullName(request.getFullName());
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(User.Role.valueOf(request.getRole()));
        user.setEnabled(true); // Default to enabled

        return userRepository.save(user);
    }

    public User createUserFromRequest(RegisterRequest request) {
        try {
            return createUser(request);
        } catch (UsernameExistsException ex) {
            // Log the specific conflict
            log.warn("Registration failed - username already exists: {}", request.getUsername());
            throw ex;
        } catch (Exception ex) {
            log.error("User creation failed for {}", request.getUsername(), ex);
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



    public User getUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("User not found with ID: " + userId));
    }

    public User updateUserRole(Long userId, User.Role newRole) {
        User user = getUserById(userId);
        user.setRole(newRole);
        return userRepository.save(user);
    }

    public List<User> getUsersByRole(User.Role role) {
        return userRepository.findByRole(role);
    }

    public void deactivateUser(Long userId) {
        User user = getUserById(userId);
        user.setEnabled(false);
        userRepository.save(user);
    }

    public void activateUser(Long userId) {
        User user = getUserById(userId);
        user.setEnabled(true);
        userRepository.save(user);
    }

    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("User not found with username: " + username));
    }

    public List<User> getActiveDoctors() {
        return userRepository.findByRoleAndEnabled(User.Role.DOCTOR, true);
    }

    public long countAdmins() {
        return userRepository.countByRole(User.Role.ADMIN);
    }

    public List<User> searchUsers(String query) {
        return userRepository.searchByUsername(query);
    }

}