package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.request.ReportCreateRequest;
import com.diagnostic.mammogram.dto.request.ReportUpdateRequest;
import com.diagnostic.mammogram.dto.response.ReportResponse;

import java.util.List;

/**
 * Interface for managing medical reports.
 * Provides methods for creating, retrieving, updating, and deleting reports.
 */
public interface ReportService {

    /**
     * Creates a new medical report based on the provided request data.
     *
     * @param request The DTO containing data for the new report.
     * @return The DTO representing the created report.
     */
    ReportResponse createReport(ReportCreateRequest request);

    /**
     * Retrieves a medical report by its unique ID.
     *
     * @param reportId The ID of the report to retrieve.
     * @return The DTO representing the retrieved report.
     * @throws com.diagnostic.mammogram.exception.ResourceNotFoundException if the report is not found.
     */
    ReportResponse getReportById(Long reportId);

    /**
     * Updates an existing medical report identified by its ID.
     *
     * @param reportId The ID of the report to update.
     * @param request The DTO containing updated data for the report.
     * @return The DTO representing the updated report.
     * @throws com.diagnostic.mammogram.exception.ResourceNotFoundException if the report is not found.
     */
    ReportResponse updateReport(Long reportId, ReportUpdateRequest request);

    /**
     * Deletes a medical report by its unique ID.
     *
     * @param reportId The ID of the report to delete.
     * @throws com.diagnostic.mammogram.exception.ResourceNotFoundException if the report is not found.
     */
    void deleteReport(Long reportId);

    /**
     * Retrieves all medical reports.
     *
     * @return A list of DTOs representing all reports.
     */
    List<ReportResponse> getAllReports();

    /**
     * Retrieves all medical reports associated with a specific mammogram.
     * Since a mammogram has one report (OneToOne), this will return a list with at most one element.
     *
     * @param mammogramId The ID of the mammogram.
     * @return A list of DTOs representing reports for the given mammogram.
     */
    List<ReportResponse> getReportsByMammogramId(Long mammogramId);

    /**
     * Retrieves all medical reports for a specific patient.
     *
     * @param patientId The ID of the patient.
     * @return A list of DTOs representing reports for the given patient.
     */
    List<ReportResponse> getReportsByPatientId(Long patientId);

    /**
     * Retrieves all medical reports created by a specific user (radiologist/doctor).
     *
     * @param userId The ID of the user who created the reports.
     * @return A list of DTOs representing reports created by the given user.
     */
    List<ReportResponse> getReportsByCreatedByUserId(Long userId);
}