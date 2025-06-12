package com.diagnostic.mammogram.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for creating a new medical report.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportCreateRequest {

    @NotNull(message = "Mammogram ID is required.")
    private Long mammogramId; // The ID of the mammogram this report is for

    @NotNull(message = "Radiologist/Doctor ID is required.")
    private Long createdByUserId; // The ID of the user (radiologist/doctor) creating the report

    @NotBlank(message = "Findings cannot be empty.")
    private String findings; // Detailed findings from the mammogram analysis

    @NotBlank(message = "Conclusion cannot be empty.")
    private String conclusion; // Overall conclusion based on findings

    @NotBlank(message = "Recommendation cannot be empty.")
    private String recommendation; // Clinical recommendations (e.g., follow-up, biopsy)

    // Initial status can be implicitly DRAFT, or optionally included if creation can set other statuses.
    // Let's assume it defaults to DRAFT on creation for now.
}