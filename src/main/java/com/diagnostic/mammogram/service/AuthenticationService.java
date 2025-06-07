package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.request.AuthenticationRequest;
import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.dto.response.AuthenticationResponse;
import com.diagnostic.mammogram.exception.InvalidRoleException;
import com.diagnostic.mammogram.exception.UsernameExistsException;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.repository.UserRepository;
import com.diagnostic.mammogram.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;

    public AuthenticationResponse register(RegisterRequest request) {
        // Validate username availability
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameExistsException(
                    "Username '" + request.getUsername() + "' already exists");
        }

        try {
            // Build and save user
            var user = User.builder()
                    .username(request.getUsername().trim())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(request.getRoleAsEnum())
                    .build();

            userRepository.save(user);

            // Generate JWT token
            var jwtToken = jwtService.generateToken(user);
            return AuthenticationResponse.builder()
                    .token(jwtToken)
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .build();

        } catch (IllegalArgumentException e) {
            throw new InvalidRoleException("Invalid role specified: " + request.getRole());
        }
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        try {
            // First check if user exists and is enabled
            var user = userRepository.findByUsername(request.getUsername())
                    .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));

            if (!user.isEnabled()) {
                throw new DisabledException("Account is disabled. Please contact support.");
            }

            // Then authenticate credentials
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsername(),
                            request.getPassword()
                    )
            );

            return AuthenticationResponse.builder()
                    .token(jwtService.generateToken(user))
                    .username(user.getUsername())
                    .role(user.getRole().name())
                    .build();

        } catch (BadCredentialsException e) {
            throw new BadCredentialsException("Invalid username or password");
        } catch (DisabledException e) {
            throw new DisabledException(e.getMessage()); // Re-throw with custom message
        }
    }
}