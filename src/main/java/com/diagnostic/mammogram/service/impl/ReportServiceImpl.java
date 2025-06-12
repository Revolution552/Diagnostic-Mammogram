package com.diagnostic.mammogram.service.impl;

import com.diagnostic.mammogram.dto.request.ReportCreateRequest;
import com.diagnostic.mammogram.dto.request.ReportUpdateRequest;
import com.diagnostic.mammogram.dto.response.ReportResponse;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.Mammogram;
import com.diagnostic.mammogram.model.Report;
import com.diagnostic.mammogram.model.ReportStatus;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.repository.MammogramRepository;
import com.diagnostic.mammogram.repository.ReportRepository;
import com.diagnostic.mammogram.repository.UserRepository;
import com.diagnostic.mammogram.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor // Lombok: Injects final fields via constructor
@Slf4j // Lombok: For logging
@Transactional // Ensures transactional integrity for methods that modify data
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final MammogramRepository mammogramRepository;
    private final UserRepository userRepository; // Assuming you have a UserRepository for fetching Users

    @Override
    public ReportResponse createReport(ReportCreateRequest request) {
        log.info("Attempting to create report for mammogram ID: {}", request.getMammogramId());

        Mammogram mammogram = mammogramRepository.findById(request.getMammogramId())
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram", "id", request.getMammogramId()));

        User createdBy = userRepository.findById(request.getCreatedByUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User (Creator)", "id", request.getCreatedByUserId()));

        // Check if a report already exists for this mammogram (due to OneToOne unique constraint)
        if (reportRepository.findByMammogram(mammogram).isPresent()) {
            throw new IllegalArgumentException("A report already exists for mammogram ID: " + request.getMammogramId());
        }

        Report report = Report.builder()
                .mammogram(mammogram)
                .createdBy(createdBy)
                .findings(request.getFindings())
                .conclusion(request.getConclusion())
                .recommendation(request.getRecommendation())
                .status(ReportStatus.DRAFT) // New reports start as DRAFT
                .build();

        Report savedReport = reportRepository.save(report);
        log.info("Report created successfully with ID: {}", savedReport.getId());
        return mapToReportResponse(savedReport);
    }

    @Override
    @Transactional(readOnly = true) // Read-only transaction for better performance
    public ReportResponse getReportById(Long reportId) {
        log.info("Attempting to retrieve report by ID: {}", reportId);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        return mapToReportResponse(report);
    }

    @Override
    public ReportResponse updateReport(Long reportId, ReportUpdateRequest request) {
        log.info("Attempting to update report with ID: {}", reportId);
        Report existingReport = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));

        // Apply partial updates
        if (request.getFindings() != null) {
            existingReport.setFindings(request.getFindings());
        }
        if (request.getConclusion() != null) {
            existingReport.setConclusion(request.getConclusion());
        }
        if (request.getRecommendation() != null) {
            existingReport.setRecommendation(request.getRecommendation());
        }
        if (request.getStatus() != null) {
            // Optional: Add logic to validate status transitions if needed
            // e.g., if (existingReport.getStatus() == ReportStatus.FINALIZED && request.getStatus() == ReportStatus.DRAFT) {
            //    throw new IllegalArgumentException("Cannot change a FINALIZED report back to DRAFT.");
            // }
            existingReport.setStatus(request.getStatus());
        }

        Report updatedReport = reportRepository.save(existingReport);
        log.info("Report ID {} updated successfully.", updatedReport.getId());
        return mapToReportResponse(updatedReport);
    }

    @Override
    public void deleteReport(Long reportId) {
        log.info("Attempting to delete report by ID: {}", reportId);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new ResourceNotFoundException("Report", "id", reportId));
        reportRepository.delete(report);
        log.info("Report ID {} deleted successfully.", reportId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getAllReports() {
        log.info("Retrieving all reports.");
        return reportRepository.findAll().stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByMammogramId(Long mammogramId) {
        log.info("Retrieving report for mammogram ID: {}", mammogramId);
        Mammogram mammogram = mammogramRepository.findById(mammogramId)
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram", "id", mammogramId));

        // Since it's a OneToOne, findByMammogram returns Optional<Report>
        return reportRepository.findByMammogram(mammogram)
                .map(this::mapToReportResponse)
                .map(Collections::singletonList) // Convert single item to a list
                .orElse(Collections.emptyList()); // Return empty list if no report found
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByPatientId(Long patientId) {
        log.info("Retrieving reports for patient ID: {}", patientId);
        return reportRepository.findByMammogramPatientId(patientId).stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportResponse> getReportsByCreatedByUserId(Long userId) {
        log.info("Retrieving reports created by user ID: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        return reportRepository.findByCreatedBy(user).stream()
                .map(this::mapToReportResponse)
                .collect(Collectors.toList());
    }

    /**
     * Helper method to map a Report entity to a ReportResponse DTO.
     * @param report The Report entity.
     * @return The corresponding ReportResponse DTO.
     */
    private ReportResponse mapToReportResponse(Report report) {
        // Ensure relationships are eagerly loaded if needed for DTO mapping outside a transaction
        // Or access them within a @Transactional(readOnly = true) method
        Mammogram mammogram = report.getMammogram(); // Accessing LAZY loaded Mammogram
        User createdBy = report.getCreatedBy(); // Accessing LAZY loaded User

        return ReportResponse.builder()
                .id(report.getId())
                .mammogramId(mammogram != null ? mammogram.getId() : null)
                .mammogramImagePath(mammogram != null ? mammogram.getImagePath() : null)
                .mammogramUploadDate(mammogram != null ? mammogram.getUploadDate() : null)
                .patientId(mammogram != null && mammogram.getPatient() != null ? mammogram.getPatient().getId() : null)
                .patientName(mammogram != null && mammogram.getPatient() != null ? mammogram.getPatient().getFullName() : null)
                .createdByUserId(createdBy != null ? createdBy.getId() : null)
                .createdByUserFullName(createdBy != null ? createdBy.getUsername() : null)
                .createdByUserRole(createdBy != null && createdBy.getRole() != null ? createdBy.getRole().name() : null) // Assuming role is an Enum
                .findings(report.getFindings())
                .conclusion(report.getConclusion())
                .recommendation(report.getRecommendation())
                .status(report.getStatus())
                .reportDate(report.getReportDate())
                .lastUpdated(report.getLastUpdated())
                .build();
    }
}