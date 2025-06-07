package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.response.ReportPdfResponse;
import com.diagnostic.mammogram.exception.ReportGenerationException;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.service.ReportService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
@Slf4j
public class ReportController {

    private final ReportService reportService;

    @GetMapping("/mammogram/{mammogramId}/pdf")
    public ResponseEntity<Resource> generatePdfReport(@PathVariable Long mammogramId) {
        try {
            ReportPdfResponse reportPdfResponse = reportService.generatePdfReport(mammogramId);

            ByteArrayResource resource = new ByteArrayResource(reportPdfResponse.getContent());

            ContentDisposition contentDisposition = ContentDisposition.builder("attachment")
                    .filename(reportPdfResponse.getFilename())
                    .build();

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition.toString())
                    .contentType(MediaType.APPLICATION_PDF)
                    .contentLength(reportPdfResponse.getFileSize())
                    .body(resource);

        } catch (ResourceNotFoundException ex) {
            log.error("Report not found for mammogram ID: {}", mammogramId, ex);
            throw ex;
        } catch (ReportGenerationException ex) {
            log.error("Failed to generate PDF report for mammogram ID: {}", mammogramId, ex);
            throw ex;
        } catch (Exception ex) {
            log.error("Unexpected error generating report for mammogram ID: {}", mammogramId, ex);
            throw new ReportGenerationException("Unexpected error generating report", ex);
        }
    }
}