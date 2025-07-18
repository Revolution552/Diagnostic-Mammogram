package com.diagnostic.mammogram.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.fasterxml.jackson.databind.ObjectMapper; // Import ObjectMapper
import com.fasterxml.jackson.core.type.TypeReference; // Import TypeReference

import java.time.LocalDateTime;
import java.util.List;
import java.util.Collections; // Import Collections for emptyList

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

    // NEW: Direct patient ID for easier access/querying, though redundant with mammogram.patient.id
    @Column(nullable = false) // Assuming patientId should always be present
    private Long patientId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "mammogram_id", referencedColumnName = "id", nullable = false, unique = true)
    private Mammogram mammogram;

    @Column(nullable = false)
    private String prediction; // e.g., "NORMAL", "ABNORMAL"

    @Column(nullable = false, length = 500) // probabilities_json cannot be null
    private String probabilitiesJson; // Store List<Double> as JSON string

    @Column(nullable = false)
    private Double confidenceScore; // Derived from prediction and probabilities

    @Column(nullable = true, length = 1000)
    private String detailedFindings; // Optional: If AI provides more detailed text

    @Column(nullable = true, length = 1000)
    private String recommendation; // Optional: If AI provides specific recommendation text

    @Column(nullable = false)
    private LocalDateTime analysisDate; // When the AI analysis was performed

    @Transient
    private List<Double> probabilitiesList; // Transient field for easy List<Double> access

    // ObjectMapper instance for JSON conversions
    private static final ObjectMapper objectMapper = new ObjectMapper();

    // --- Lifecycle Callbacks for probabilitiesList conversion ---
    @PostLoad
    @PostPersist
    @PostUpdate
    private void convertProbabilitiesJsonToList() {
        if (this.probabilitiesJson != null && !this.probabilitiesJson.isEmpty()) {
            try {
                this.probabilitiesList = objectMapper.readValue(this.probabilitiesJson, new TypeReference<List<Double>>() {});
            } catch (Exception e) {
                System.err.println("Error converting probabilities JSON to List: " + e.getMessage());
                this.probabilitiesList = Collections.emptyList(); // Set to empty list on error
            }
        } else {
            this.probabilitiesList = Collections.emptyList(); // Set to empty list if JSON is null/empty
        }
    }

    @PrePersist
    @PreUpdate
    private void convertProbabilitiesListToJson() {
        if (this.probabilitiesList != null) { // Check if list itself is not null
            try {
                // Convert to JSON string. If list is empty, it will be "[]"
                this.probabilitiesJson = objectMapper.writeValueAsString(this.probabilitiesList);
            } catch (Exception e) {
                System.err.println("Error converting probabilities List to JSON: " + e.getMessage());
                // If conversion fails, set to an empty JSON array string instead of null
                this.probabilitiesJson = "[]";
            }
        } else {
            // If probabilitiesList is null, store an empty JSON array string to satisfy NOT NULL constraint
            this.probabilitiesJson = "[]";
        }
    }
}
