package com.diagnostic.mammogram.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "ai_diagnosis_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIDiagnosisResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // One-to-one relationship with Mammogram
    // A Mammogram can have one AI diagnosis result, and an AI diagnosis result belongs to one Mammogram.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mammogram_id", referencedColumnName = "id", nullable = false, unique = true)
    private Mammogram mammogram;

    @Column(nullable = false)
    private String prediction; // e.g., "NORMAL", "ABNORMAL"

    // Store probabilities as a JSON string or a separate table if complex.
    // For simplicity, we'll store it as a comma-separated string or JSON string.
    // Using @Convert to handle List<Double> to String conversion for simplicity.
    // You'll need to create a converter for this.
    @Column(nullable = false, length = 500) // Increased length for JSON string
    private String probabilitiesJson; // Store List<Double> as JSON string

    @Column(nullable = false)
    private Double confidenceScore; // Derived from prediction and probabilities

    @Column(nullable = true, length = 1000)
    private String detailedFindings; // Optional: If AI provides more detailed text

    @Column(nullable = true, length = 1000)
    private String recommendation; // Optional: If AI provides specific recommendation text

    @Column(nullable = false)
    private LocalDateTime analysisDate; // When the AI analysis was performed

    // Transient field for easy access to List<Double>
    @Transient
    private List<Double> probabilitiesList;

    // --- Lifecycle Callbacks for probabilitiesList conversion ---
    @PostLoad
    @PostPersist
    @PostUpdate
    private void convertProbabilitiesJsonToList() {
        if (this.probabilitiesJson != null && !this.probabilitiesJson.isEmpty()) {
            // Using Jackson for JSON conversion. Ensure you have Jackson dependencies.
            // <dependency>
            //    <groupId>com.fasterxml.jackson.core</groupId>
            //    <artifactId>jackson-databind</artifactId>
            // </dependency>
            try {
                this.probabilitiesList = new com.fasterxml.jackson.databind.ObjectMapper().readValue(this.probabilitiesJson, new com.fasterxml.jackson.core.type.TypeReference<List<Double>>() {});
            } catch (Exception e) {
                // Log error
                System.err.println("Error converting probabilities JSON to List: " + e.getMessage());
                this.probabilitiesList = null;
            }
        } else {
            this.probabilitiesList = null;
        }
    }

    @PrePersist
    @PreUpdate
    private void convertProbabilitiesListToJson() {
        if (this.probabilitiesList != null && !this.probabilitiesList.isEmpty()) {
            try {
                this.probabilitiesJson = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(this.probabilitiesList);
            } catch (Exception e) {
                // Log error
                System.err.println("Error converting probabilities List to JSON: " + e.getMessage());
                this.probabilitiesJson = null;
            }
        } else {
            this.probabilitiesJson = null;
        }
    }
}