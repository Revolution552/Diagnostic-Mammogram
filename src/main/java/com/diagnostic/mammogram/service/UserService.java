package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.request.RegisterRequest;
import com.diagnostic.mammogram.exception.UserNotFoundException;
import com.diagnostic.mammogram.exception.UsernameExistsException;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public User createUser(String username, String password, User.Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new UsernameExistsException(username);
        }

        User user = User.builder()
                .username(username)
                .password(passwordEncoder.encode(password))
                .role(role)
                .enabled(true)
                .build();

        return userRepository.save(user);
    }

    public User createUserFromRequest(RegisterRequest request) {
        return createUser(
                request.getUsername(),
                request.getPassword(),
                request.getRoleAsEnum()
        );
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