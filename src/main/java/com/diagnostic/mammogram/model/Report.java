package com.diagnostic.mammogram.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity // Marks this class as a JPA entity
@Table(name = "reports") // Defines the table name in the database
@Data // Lombok: Generates getters, setters, toString, equals, hashCode
@NoArgsConstructor // Lombok: Generates a no-argument constructor
@AllArgsConstructor // Lombok: Generates an all-argument constructor
@Builder // Lombok: Provides a builder pattern for object creation
public class Report {

    @Id // Marks this field as the primary key
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-increments ID
    private Long id;

    // One-to-One relationship with Mammogram
    // A report is specifically for one mammogram.
    // Use mappedBy on the owning side (Mammogram if Report doesn't have Mammogram ID as FK directly)
    // Or simpler, let Report own the Mammogram relationship.
    @OneToOne(fetch = FetchType.LAZY) // Use LAZY to avoid fetching mammogram unless explicitly needed
    @JoinColumn(name = "mammogram_id", referencedColumnName = "id", nullable = false, unique = true) // Foreign key to mammogram table
    private Mammogram mammogram;

    // Many-to-One relationship with User (Radiologist/Doctor)
    // A report is created by one user, and a user can create many reports.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", referencedColumnName = "id", nullable = false)
    private User createdBy; // The user who created this report

    @Column(columnDefinition = "TEXT") // Use TEXT for potentially long strings
    private String findings; // Detailed findings from the mammogram analysis

    @Column(columnDefinition = "TEXT")
    private String conclusion; // Overall conclusion based on findings

    @Column(columnDefinition = "TEXT")
    private String recommendation; // Clinical recommendations (e.g., follow-up, biopsy)

    @Enumerated(EnumType.STRING) // Store enum names as strings in DB
    @Column(nullable = false)
    private ReportStatus status; // Current status of the report (DRAFT, FINALIZED, etc.)

    @CreationTimestamp // Automatically sets creation timestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime reportDate; // Date when the report was first created

    @UpdateTimestamp // Automatically updates modification timestamp
    private LocalDateTime lastUpdated; // Date when the report was last modified

    // Optional: Fields to store key AI diagnosis results if not linked via Mammogram or a separate AIResult entity
    // For simplicity, we can include summary AI details here or link to the AIDiagnosis when processing.
    // For now, assume AI analysis happens on mammogram upload and its results are considered when creating report.
    // If you need to persist AI diagnosis specific to this report, you might add:
    // @Column(columnDefinition = "TEXT")
    // private String aiDiagnosisSummary;
    // private Double aiConfidenceScore;
}