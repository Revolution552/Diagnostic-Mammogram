package com.diagnostic.mammogram.dto.response;

import com.diagnostic.mammogram.dto.AIDiagnosis;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MammogramResponse {
    private Long id;
    private Long patientId; // Just the ID, not the full patient object for simplicity in response
    private String patientName; // Potentially include patient name for convenience
    private String imagePath;
    private LocalDateTime dateUploaded;
    private String notes;
    private AIDiagnosis aiDiagnosis; // Include AI diagnosis results if available for this mammogram
}