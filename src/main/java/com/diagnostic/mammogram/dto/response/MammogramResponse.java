package com.diagnostic.mammogram.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime; // Use LocalDateTime for date/time fields
import java.util.List; // For probabilities

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MammogramResponse {
    private Long id;
    private Long patientId;
    private String patientName; // NEW: Added patientName for frontend display
    private String imagePath; // URL to the stored image
    private String notes;
    private LocalDateTime dateUploaded; // Use LocalDateTime, will be converted to ISO string by Jackson

    // Nested DTO for AI Diagnosis results
    private AiDiagnosisResponse aiDiagnosis; // Can be null if no diagnosis yet

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiDiagnosisResponse {
        private String diagnosisSummary;
        private String prediction;
        private Double confidenceScore;
        private List<Double> probabilities; // Assuming probabilities are doubles
    }
}
