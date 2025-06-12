package com.diagnostic.mammogram.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AIDiagnosis {
    private String diagnosisSummary;
    private Double confidenceScore;
    private String detailedFindings;
    private String recommendation; // AI's recommendation, can be used to pre-fill report
    // Add any other relevant AI output fields, e.g., image markers, probabilities for different conditions
    // private List<Map<String, Object>> lesionLocations;
    // private Map<String, Double> probabilities;
}