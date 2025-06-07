package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.response.ReportPdfResponse;
import com.diagnostic.mammogram.exception.ReportGenerationException;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.Report;
import com.diagnostic.mammogram.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ReportService {

    private static final float MARGIN_X = 50;
    private static final float MARGIN_Y = 50;
    private static final float LINE_HEIGHT = 15;
    private static final float TITLE_FONT_SIZE = 14;
    private static final float HEADER_FONT_SIZE = 12;
    private static final float BODY_FONT_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final ReportRepository reportRepository;
    private final PdfTemplateService pdfTemplateService;

    public ReportPdfResponse generatePdfReport(Long mammogramId) {
        Report report = reportRepository.findWithDetailsByMammogramId(mammogramId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Report not found for mammogram ID: " + mammogramId));

        try {
            byte[] pdfContent = generatePdfContent(report);
            log.info("Successfully generated PDF report for mammogram ID: {}", mammogramId);
            return new ReportPdfResponse(
                    report.getId(),
                    "mammogram_report_" + mammogramId + ".pdf",
                    pdfContent.length,
                    pdfContent
            );
        } catch (IOException e) {
            log.error("PDF generation failed for mammogram ID: {}", mammogramId, e);
            throw new ReportGenerationException("Failed to generate PDF report", e);
        }
    }

    private byte[] generatePdfContent(Report report) throws IOException {
        // Directly use LocalDateTime without conversion
        LocalDateTime createdAt = report.getCreatedAt();

        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                float currentY = page.getMediaBox().getHeight() - MARGIN_Y - 50;

                // Add report header
                currentY = addTitle(contentStream, page, "Mammogram Diagnostic Report", currentY);
                currentY -= LINE_HEIGHT * 2;

                // Add patient information
                currentY = addSectionHeader(contentStream, "Patient Information", currentY);
                currentY = addKeyValueLine(contentStream, "Name:",
                        report.getMammogram().getPatient().getName(), currentY);
                currentY = addKeyValueLine(contentStream, "Age:",
                        String.valueOf(report.getMammogram().getPatient().getAge()), currentY);
                currentY = addKeyValueLine(contentStream, "Report Date:",
                        createdAt.format(DATE_FORMATTER), currentY);
                currentY -= LINE_HEIGHT;

                // Add findings
                currentY = addSectionHeader(contentStream, "Findings", currentY);
                currentY = addMultilineText(contentStream, report.getFindings(), currentY);
                currentY -= LINE_HEIGHT;

                // Add recommendations
                currentY = addSectionHeader(contentStream, "Recommendations", currentY);
                addMultilineText(contentStream, report.getRecommendations(), currentY);
            }

            // Apply template
            pdfTemplateService.applyReportTemplate(
                    document,
                    report.getMammogram().getPatient().getName(),
                    report.getId().toString()
            );

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }
    private float addTitle(PDPageContentStream contentStream, PDPage page,
                           String text, float y) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, TITLE_FONT_SIZE);
        float titleWidth = PDType1Font.HELVETICA_BOLD.getStringWidth(text) / 1000 * TITLE_FONT_SIZE;
        float titleX = (page.getMediaBox().getWidth() - titleWidth) / 2;

        contentStream.beginText();
        contentStream.newLineAtOffset(titleX, y);
        contentStream.showText(text);
        contentStream.endText();
        return y - LINE_HEIGHT;
    }

    private float addSectionHeader(PDPageContentStream contentStream,
                                   String text, float y) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, HEADER_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_X, y);
        contentStream.showText(text);
        contentStream.endText();

        // Add underline
        contentStream.moveTo(MARGIN_X, y - 2);
        contentStream.lineTo(MARGIN_X + 100, y - 2);
        contentStream.stroke();

        return y - LINE_HEIGHT;
    }

    private float addKeyValueLine(PDPageContentStream contentStream,
                                  String key, String value, float y) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, BODY_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_X, y);
        contentStream.showText(key);
        contentStream.endText();

        contentStream.setFont(PDType1Font.HELVETICA, BODY_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_X + 100, y);
        contentStream.showText(value);
        contentStream.endText();

        return y - LINE_HEIGHT;
    }

    private float addMultilineText(PDPageContentStream contentStream,
                                   String text, float y) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA, BODY_FONT_SIZE);
        String[] lines = text.split("\n");

        for (String line : lines) {
            if (y < MARGIN_Y) {
                // Handle page overflow (would need to create new page in real implementation)
                break;
            }
            contentStream.beginText();
            contentStream.newLineAtOffset(MARGIN_X, y);
            contentStream.showText(line);
            contentStream.endText();
            y -= LINE_HEIGHT;
        }

        return y;
    }
}