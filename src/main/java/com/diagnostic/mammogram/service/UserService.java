package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.exception.UserCreationException;
import com.diagnostic.mammogram.exception.UserNotFoundException;
import com.diagnostic.mammogram.exception.UsernameExistsException;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(RegisterRequest request) {
        log.info("Creating user: {}", request.getUsername()); // Now works
        // Check username availability first
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new UsernameExistsException(request.getUsername());
        }

        // Create and save new user
        User user = new User();
        user.setUsername(request.getUsername());
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