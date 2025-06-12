package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.response.ReportResponse;
import com.diagnostic.mammogram.exception.ImageStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.xhtmlrenderer.pdf.ITextRenderer; // From openhtmltopdf-pdfbox

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import com.lowagie.text.DocumentException; // IMPORTANT: Add this import!

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportGeneratorService {

    private final ReportService reportService;
    private final SpringTemplateEngine templateEngine;

    /**
     * Generates a PDF report for a given mammogram report ID.
     *
     * @param reportId The ID of the report to generate PDF for.
     * @return A byte array containing the generated PDF document.
     * @throws ImageStorageException if report data cannot be found or PDF generation fails.
     */
    public byte[] generatePdfReport(Long reportId) {
        log.info("Starting PDF report generation for report ID: {}", reportId);

        // 1. Fetch the report data
        ReportResponse report = reportService.getReportById(reportId);
        // This check should ideally not be needed if reportService.getReportById throws ResourceNotFoundException
        Objects.requireNonNull(report, "Report cannot be null for PDF generation.");

        // 2. Prepare Thymeleaf context with data
        Context context = new Context();
        context.setVariable("report", report);

        // 3. Process the HTML template
        String htmlContent = templateEngine.process("report_template", context);
        log.debug("HTML content generated for report ID {}: {}", reportId, htmlContent.substring(0, Math.min(htmlContent.length(), 500)) + "...");

        // 4. Convert HTML to PDF
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {
            ITextRenderer renderer = new ITextRenderer();
            renderer.setDocumentFromString(htmlContent);
            renderer.layout();
            renderer.createPDF(outputStream); // This line can throw DocumentException

            log.info("PDF report generated successfully for report ID: {}", reportId);
            return outputStream.toByteArray();

        } catch (DocumentException | IOException e) { // Catch both DocumentException and IOException
            log.error("Error generating PDF for report ID {}: {}", reportId, e.getMessage(), e);
            throw new ImageStorageException("Failed to generate PDF report for ID " + reportId, e);
        }
    }
}