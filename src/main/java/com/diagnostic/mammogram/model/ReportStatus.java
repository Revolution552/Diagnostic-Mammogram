package com.diagnostic.mammogram.model;

/**
 * Enum representing the status of a medical report.
 */
public enum ReportStatus {
    DRAFT,      // Report is being created or reviewed
    FINALIZED,  // Report has been completed and signed off
    AMENDED,    // Report has been revised after finalization
    REJECTED    // Report was deemed incorrect or invalid
}