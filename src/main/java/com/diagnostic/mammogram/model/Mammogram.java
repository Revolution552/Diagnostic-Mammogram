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

    @Lob // Used for large text fields
    private String notes; // Any additional notes or initial observations

    private String result;

    @CreationTimestamp // This typically marks the field as the creation timestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime uploadDate;
}