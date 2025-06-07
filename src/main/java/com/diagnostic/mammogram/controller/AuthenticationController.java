package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.AuthenticationRequest;
import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.dto.response.AuthenticationResponse;
import com.diagnostic.mammogram.exception.UsernameExistsException;
import com.diagnostic.mammogram.service.AuthenticationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthenticationController {

    private static final Logger logger = LoggerFactory.getLogger(AuthenticationController.class);
    private final AuthenticationService authenticationService;

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @RequestBody RegisterRequest request) {

        Map<String, Object> response = new HashMap<>();
        String username = request.getUsername();
        logger.info("Registration attempt for user: {}", username);

        try {
            AuthenticationResponse authResponse = authenticationService.register(request);

            response.put("status", "success");
            response.put("message", "User registered successfully");
            response.put("data", authResponse);

            logger.info("Registration successful for user: {}", username);
            return ResponseEntity.ok(response);

        } catch (UsernameExistsException ex) {
            logger.warn("Registration failed - username already exists: {}", username);
            response.put("status", "error");
            response.put("message", "Username already exists");
            response.put("suggestions", suggestUsernameVariations(username));
            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception ex) {
            logger.error("Registration failed for user: {} - Error: {}", username, ex.getMessage(), ex);
            response.put("status", "error");
            response.put("message", "Registration failed: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

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