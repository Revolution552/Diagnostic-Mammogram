package com.diagnostic.mammogram.model;

import jakarta.persistence.*; // Using Jakarta Persistence API (for Spring Boot 3+)
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data; // Includes @Getter, @Setter, @ToString, @EqualsAndHashCode
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Data // Generates getters, setters, toString, equals, and hashCode
@NoArgsConstructor // Generates a no-argument constructor
@AllArgsConstructor // Generates a constructor with all fields
@Builder // Provides a builder pattern for object creation
@Entity
@Table(name = "mammograms") // Explicitly names the database table
public class Mammogram {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrementing ID
    private Long id;

    // Many Mammograms can belong to one Patient
    @ManyToOne(fetch = FetchType.LAZY) // LAZY loading for performance
    @JoinColumn(name = "patient_id", nullable = false) // Foreign key column
    private Patient patient; // Associated Patient object

    @Column(nullable = false, unique = true) // Image path, must be present and unique
    private String imagePath; // Path to the stored image file (e.g., S3 URL, local file path)

    @Column(nullable = false) // Date uploaded, must be present
    private LocalDateTime dateUploaded; // Timestamp when the mammogram was uploaded

    @Column(length = 1000)
    @Lob // Used for large text fields
    private String notes; // Any additional notes or initial observations

    @CreationTimestamp // This typically marks the field as the creation timestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadDate;

    // NEW: One-to-one relationship with AIDiagnosisResult
    // mappedBy indicates that the 'mammogram' field in AIDiagnosisResult is the owning side.
    // CascadeType.ALL means if a Mammogram is deleted, its associated AIDiagnosisResult is also deleted.
    // orphanRemoval = true ensures that if the link is removed, the child entity is also removed.
    @OneToOne(mappedBy = "mammogram", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private AIDiagnosisResult aiDiagnosisResult; // Store the AI diagnosis result here
}