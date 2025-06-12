package com.diagnostic.mammogram.dto.request;

import com.diagnostic.mammogram.model.ReportStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for updating an existing medical report.
 * Fields are nullable to allow partial updates.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReportUpdateRequest {

    private String findings; // Updated detailed findings

    private String conclusion; // Updated overall conclusion

    private String recommendation; // Updated clinical recommendations

    private ReportStatus status; // Updated status of the report (e.g., FINALIZED, AMENDED)
}