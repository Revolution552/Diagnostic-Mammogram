package com.diagnostic.mammogram.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
@Entity
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String token; // 6-digit code

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public void generateTimestamps(int minutesToExpire) {
        this.expiryDate = LocalDateTime.now().plusMinutes(minutesToExpire);
    }

    public boolean isExpired() {
        return expiryDate.isBefore(LocalDateTime.now());
    }

    // Getters and Setters...
}
