package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.response.ReportPdfResponse;
import com.diagnostic.mammogram.exception.ReportGenerationException;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/mammogram/{mammogramId}/pdf")
    public ResponseEntity<?> generatePdfReport(@PathVariable Long mammogramId) {
        Map<String, Object> response = new HashMap<>();
        log.info("Received request to generate PDF report for mammogram ID: {}", mammogramId);

        try {
            ReportPdfResponse reportPdfResponse = reportService.generatePdfReport(mammogramId);
            log.debug("PDF report generated successfully for mammogram ID: {}", mammogramId);
            log.info("Report details - Filename: {}, Size: {} bytes",
                    reportPdfResponse.getFilename(), reportPdfResponse.getFileSize());

            ByteArrayResource resource = new ByteArrayResource(reportPdfResponse.getContent());

            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(reportPdfResponse.getFilename())
                    .build();

            // Success response for PDF file
            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(reportPdfResponse.getFileSize())
                    .body(resource);

        } catch (ResourceNotFoundException ex) {
            String errorMessage = String.format("Mammogram not found with ID: %d", mammogramId);
            log.error("{}: {}", errorMessage, ex.getMessage());

            response.put("status", "error");
            response.put("errorCode", "RESOURCE_NOT_FOUND");
            response.put("message", errorMessage);
            response.put("details", "Please verify the mammogram ID and try again");

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (ReportGenerationException ex) {
            String errorMessage = String.format("Report generation failed for mammogram ID: %d", mammogramId);
            log.error("{}: {}", errorMessage, ex.getMessage(), ex);

            response.put("status", "error");
            response.put("errorCode", "REPORT_GENERATION_FAILED");
            response.put("message", errorMessage);
            response.put("details", ex.getCause() != null ? ex.getCause().getMessage() : "Technical error during report generation");

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);

        } catch (Exception ex) {
            String errorMessage = "Unexpected error occurred during report generation";
            log.error("{} for mammogram ID {}: {}", errorMessage, mammogramId, ex.getMessage(), ex);

            response.put("status", "error");
            response.put("errorCode", "INTERNAL_SERVER_ERROR");
            response.put("message", errorMessage);
            response.put("details", "Please contact support with this reference: " + System.currentTimeMillis());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}