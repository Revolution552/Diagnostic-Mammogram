package com.diagnostic.mammogram.repository;

import com.diagnostic.mammogram.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    // Authentication-related queries
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);

    // Role-based queries
    List<User> findByRole(User.Role role);
    long countByRole(User.Role role);

    // Status-based queries
    List<User> findByEnabled(boolean enabled);
    List<User> findByRoleAndEnabled(User.Role role, boolean enabled);

    // Custom update query
    @Modifying
    @Query("UPDATE User u SET u.enabled = :enabled WHERE u.id = :userId")
    int updateUserStatus(@Param("userId") Long userId, @Param("enabled") boolean enabled);

    // Search functionality
    @Query("SELECT u FROM User u WHERE LOWER(u.username) LIKE LOWER(concat('%', :query, '%'))")
    List<User> searchByUsername(@Param("query") String query);
}