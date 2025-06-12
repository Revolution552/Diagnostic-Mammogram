package com.diagnostic.mammogram.dto.response;

import com.diagnostic.mammogram.model.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * DTO for sending medical report details as a response.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportResponse {

    private Long id; // Report ID

    // Condensed Mammogram info for quick reference
    private Long mammogramId;
    private String mammogramImagePath; // The URL/path to the image
    private LocalDateTime mammogramUploadDate;

    // Condensed Patient info (fetched via Mammogram)
    private Long patientId;
    private String patientName;

    // Condensed User (Creator) info
    private Long createdByUserId;
    private String createdByUserFullName;
    private String createdByUserRole;

    private String findings; // Detailed findings
    private String conclusion; // Overall conclusion
    private String recommendation; // Clinical recommendations
    private ReportStatus status; // Current status of the report

    private LocalDateTime reportDate; // When the report was first created
    private LocalDateTime lastUpdated; // When the report was last updated

    // You might add AI diagnosis summary here if it's part of the final report output
    // private String aiDiagnosisSummary;
    // private Double aiConfidenceScore;
}