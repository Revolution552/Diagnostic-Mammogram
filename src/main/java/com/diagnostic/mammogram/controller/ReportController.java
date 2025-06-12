package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.ReportCreateRequest;
import com.diagnostic.mammogram.dto.request.ReportUpdateRequest;
import com.diagnostic.mammogram.dto.response.ReportResponse;
import com.diagnostic.mammogram.service.ReportGeneratorService; // New Import
import com.diagnostic.mammogram.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders; // New Import
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType; // New Import
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/reports") // Base path for report API endpoints
@RequiredArgsConstructor // Lombok: Injects final fields via constructor
@Slf4j // Lombok: For logging
public class ReportController {

    private final ReportService reportService;
    private final ReportGeneratorService reportGeneratorService; // Inject the new service

    /**
     * Creates a new medical report.
     * Accessible by RADIOLOGIST, DOCTOR, ADMIN.
     *
     * @param request The DTO containing data for the new report.
     * @return ResponseEntity with a Map representing the created report and HTTP status 201 (Created).
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('RADIOLOGIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> createReport(@Valid @RequestBody ReportCreateRequest request) {
        log.info("Received request to create report for mammogram ID: {}", request.getMammogramId());
        ReportResponse reportResponse = reportService.createReport(request);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Report created successfully.");
        responseMap.put("data", reportResponse);
        responseMap.put("status", HttpStatus.CREATED.value());

        return new ResponseEntity<>(responseMap, HttpStatus.CREATED);
    }

    /**
     * Retrieves a medical report by its unique ID.
     * Accessible by RADIOLOGIST, DOCTOR, ADMIN.
     *
     * @param reportId The ID of the report to retrieve.
     * @return ResponseEntity with a Map representing the retrieved report and HTTP status 200 (OK).
     */
    @GetMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('RADIOLOGIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getReportById(@PathVariable Long reportId) {
        log.info("Received request to get report by ID: {}", reportId);
        ReportResponse reportResponse = reportService.getReportById(reportId);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Report retrieved successfully.");
        responseMap.put("data", reportResponse);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Updates an existing medical report.
     * Accessible by RADIOLOGIST, DOCTOR, ADMIN.
     *
     * @param reportId The ID of the report to update.
     * @param request The DTO containing updated data for the report.
     * @return ResponseEntity with a Map representing the updated report and HTTP status 200 (OK).
     */
    @PutMapping("/{reportId}")
    @PreAuthorize("hasAnyRole('RADIOLOGIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> updateReport(
            @PathVariable Long reportId,
            @RequestBody ReportUpdateRequest request) { // @Valid not used for partial updates
        log.info("Received request to update report ID: {}", reportId);
        ReportResponse reportResponse = reportService.updateReport(reportId, request);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Report updated successfully.");
        responseMap.put("data", reportResponse);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Deletes a medical report by its unique ID.
     * Accessible by ADMIN.
     *
     * @param reportId The ID of the report to delete.
     * @return ResponseEntity with a Map confirming deletion and HTTP status 204 (No Content).
     */
    @DeleteMapping("/{reportId}")
    @PreAuthorize("hasRole('ADMIN')") // Typically, only Admins delete reports
    public ResponseEntity<Map<String, Object>> deleteReport(@PathVariable Long reportId) {
        log.info("Received request to delete report by ID: {}", reportId);
        reportService.deleteReport(reportId);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Report ID " + reportId + " deleted successfully.");
        responseMap.put("status", HttpStatus.NO_CONTENT.value());

        return new ResponseEntity<>(responseMap, HttpStatus.NO_CONTENT);
    }

    /**
     * Retrieves all medical reports.
     * Accessible by RADIOLOGIST, DOCTOR, ADMIN.
     *
     * @return ResponseEntity with a Map representing a list of all reports and HTTP status 200 (OK).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('RADIOLOGIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getAllReports() {
        log.info("Received request to get all reports.");
        List<ReportResponse> reports = reportService.getAllReports();

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "All reports retrieved successfully.");
        responseMap.put("data", reports);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Retrieves all medical reports associated with a specific mammogram.
     * Accessible by RADIOLOGIST, DOCTOR, ADMIN.
     *
     * @param mammogramId The ID of the mammogram.
     * @return ResponseEntity with a Map representing reports for the given mammogram and HTTP status 200 (OK).
     */
    @GetMapping("/mammogram/{mammogramId}")
    @PreAuthorize("hasAnyRole('RADIOLOGIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getReportsByMammogramId(@PathVariable Long mammogramId) {
        log.info("Received request to get reports for mammogram ID: {}", mammogramId);
        List<ReportResponse> reports = reportService.getReportsByMammogramId(mammogramId);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Reports for mammogram " + mammogramId + " retrieved successfully.");
        responseMap.put("data", reports);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Retrieves all medical reports for a specific patient.
     * Accessible by RADIOLOGIST, DOCTOR, ADMIN.
     *
     * @param patientId The ID of the patient.
     * @return ResponseEntity with a Map representing reports for the given patient and HTTP status 200 (OK).
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('RADIOLOGIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getReportsByPatientId(@PathVariable Long patientId) {
        log.info("Received request to get reports for patient ID: {}", patientId);
        List<ReportResponse> reports = reportService.getReportsByPatientId(patientId);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Reports for patient " + patientId + " retrieved successfully.");
        responseMap.put("data", reports);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Retrieves all medical reports created by a specific user (radiologist/doctor).
     * Accessible by RADIOLOGIST, DOCTOR, ADMIN.
     *
     * @param userId The ID of the user who created the reports.
     * @return ResponseEntity with a Map representing reports created by the given user and HTTP status 200 (OK).
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('RADIOLOGIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<Map<String, Object>> getReportsByCreatedByUserId(@PathVariable Long userId) {
        log.info("Received request to get reports created by user ID: {}", userId);
        List<ReportResponse> reports = reportService.getReportsByCreatedByUserId(userId);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Reports created by user " + userId + " retrieved successfully.");
        responseMap.put("data", reports);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Generates and downloads a PDF version of a medical report.
     * Accessible by RADIOLOGIST, DOCTOR, ADMIN.
     *
     * @param reportId The ID of the report to generate the PDF for.
     * @return ResponseEntity with the PDF byte array and appropriate headers for download.
     */
    @GetMapping("/{reportId}/pdf")
    @PreAuthorize("hasAnyRole('RADIOLOGIST', 'DOCTOR', 'ADMIN')")
    public ResponseEntity<byte[]> downloadReportPdf(@PathVariable Long reportId) {
        log.info("Received request to generate PDF for report ID: {}", reportId);
        byte[] pdfBytes = reportGeneratorService.generatePdfReport(reportId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String filename = "mammogram_report_" + reportId + ".pdf";
        headers.setContentDispositionFormData("attachment", filename); // "attachment" prompts download
        headers.setContentLength(pdfBytes.length);

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}