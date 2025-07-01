package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.AuthenticationRequest;
import com.diagnostic.mammogram.dto.request.ForgotPasswordRequest;
import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.dto.request.ResetPasswordRequest;
import com.diagnostic.mammogram.dto.response.AuthenticationResponse;
import com.diagnostic.mammogram.exception.EmailFailureException;
import com.diagnostic.mammogram.exception.EmailNotFoundException;
import com.diagnostic.mammogram.exception.UsernameExistsException;
import com.diagnostic.mammogram.service.AuthenticationService;
import com.diagnostic.mammogram.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService;
    private final UserService userService;


    @PostMapping("/authenticate")
    public ResponseEntity<Map<String, Object>> authenticate(
            @RequestBody AuthenticationRequest request) {

        Map<String, Object> response = new HashMap<>();
        String username = request.getUsername();
        logger.info("Authentication attempt for user: {}", username);

        try {
            AuthenticationResponse authResponse = authenticationService.authenticate(request);

            response.put("status", "success");
            response.put("message", "Authentication successful");
            response.put("data", authResponse);

            logger.info("Authentication successful for user: {}", username);
            return ResponseEntity.ok(response);

        } catch (BadCredentialsException ex) {
            logger.warn("Authentication failed - invalid credentials for user: {}", username);
            response.put("status", "error");
            response.put("message", "Invalid username or password");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);

        } catch (DisabledException ex) {
            logger.warn("Authentication failed - disabled account: {}", username);
            response.put("status", "error");
            response.put("message", ex.getMessage());
            response.put("support_contact", "support@mammogram.com");
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);

        } catch (Exception ex) {
            logger.error("Authentication failed for user: {} - Error: {}", username, ex.getMessage(), ex);
            response.put("status", "error");
            response.put("message", "Authentication failed");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/forgot-password")
    // *** CRITICAL CHANGE HERE: Changed from @RequestParam String email to @RequestBody ForgotPasswordRequest request ***
    public ResponseEntity<Map<String, Object>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        Map<String, Object> response = new HashMap<>();
        String email = request.getEmail(); // <--- Get email from the request body DTO
        logger.info("Password reset request for email: {}", email); // Log the email

        try {
            userService.forgotPassword(email);
            logger.info("Password reset email sent to: {}", email);
            response.put("success", true);
            response.put("message", "Password reset email sent.");
            // Aligning with frontend's "status" field for consistency
            response.put("status", "success");
            return ResponseEntity.ok(response);

        } catch (EmailNotFoundException ex) {
            logger.warn("Email not found: {}", email);
            response.put("success", false);
            response.put("message", "Email not found.");
            response.put("status", "error"); // Aligning with frontend's "status" field
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (EmailFailureException ex) {
            logger.error("Password reset email failure: {}", email, ex);
            response.put("success", false);
            response.put("message", "Failed to send password reset email.");
            response.put("status", "error"); // Aligning with frontend's "status" field
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        } catch (Exception ex) { // Catch any other unexpected exceptions
            logger.error("Unexpected error during forgot password request for email: {}", email, ex);
            response.put("success", false);
            response.put("message", "An unexpected error occurred during password reset request.");
            response.put("status", "error");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, Object>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        Map<String, Object> response = new HashMap<>();

        try {
            boolean result = userService.resetPasswordWithToken(
                    request.getToken(),
                    request.getNewPassword(),
                    request.getConfirmPassword()
            );

            if (result) {
                logger.info("Password reset for token: {}", request.getToken());
                response.put("success", true);
                response.put("message", "Password reset successful.");
                response.put("status", "success"); // Aligning with frontend's "status" field
                return ResponseEntity.ok(response);
            } else {
                // This branch might be hit if userService.resetPasswordWithToken returns false
                // without throwing an exception.
                logger.warn("Password reset failed (unexpected false) for token: {}", request.getToken());
                response.put("success", false);
                response.put("message", "Password reset failed due to invalid token or other issue.");
                response.put("status", "error"); // Aligning with frontend's "status" field
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
            }

        } catch (IllegalArgumentException ex) {
            logger.warn("Password reset failed for token: {} - {}", request.getToken(), ex.getMessage());
            response.put("success", false);
            response.put("message", ex.getMessage());
            response.put("status", "error"); // Aligning with frontend's "status" field
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception ex) {
            logger.error("Password reset error for token: {}", request.getToken(), ex);
            response.put("success", false);
            response.put("message", "Unexpected error: " + ex.getMessage());
            response.put("status", "error"); // Aligning with frontend's "status" field
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    private String[] suggestUsernameVariations(String username) {
        return new String[]{
                username + "123",
                username + "_2023",
                "dr_" + username,
                username.substring(0, Math.min(username.length(), 5)) + "_" +
                        (int) (Math.random() * 1000)
        };
    }
}