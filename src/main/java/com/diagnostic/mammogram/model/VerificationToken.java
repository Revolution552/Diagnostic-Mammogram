package com.diagnostic.mammogram.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "verification_token")
public class VerificationToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Lob
    @Column(name = "token", nullable = false, unique = true)
    private String token;

    @Column(name = "created_timestamp", nullable = false)
    private Timestamp createdTimestamp;

    @Column(name = "expiry_timestamp", nullable = false)
    private Timestamp expiryTimestamp;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public void generateTimestamps(int hoursValid) {
        LocalDateTime now = LocalDateTime.now();
        this.createdTimestamp = Timestamp.valueOf(now);
        this.expiryTimestamp = Timestamp.valueOf(now.plusHours(hoursValid));
    }
}
