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
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportService {

    // Configuration constants
    private static final float MARGIN_X = 50;
    private static final float MARGIN_Y = 50;
    private static final float LINE_HEIGHT = 15;
    private static final float TITLE_FONT_SIZE = 16;
    private static final float SECTION_FONT_SIZE = 12;
    private static final float BODY_FONT_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final int MAX_LINE_WIDTH = 80;

    // Font declarations (legacy approach)
    private static final PDType1Font TITLE_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font SECTION_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font BODY_FONT = PDType1Font.HELVETICA;
    private static final PDType1Font BODY_BOLD_FONT = PDType1Font.HELVETICA_BOLD;
    private static final PDType1Font FOOTER_FONT = PDType1Font.HELVETICA_OBLIQUE;

    private final ReportRepository reportRepository;
    private final PdfTemplateService pdfTemplateService;

    @Transactional(readOnly = true)
    public ReportPdfResponse generatePdfReport(Long mammogramId) {
        log.info("Generating PDF report for mammogram ID: {}", mammogramId);

        Report report = reportRepository.findWithDetailsByMammogramId(mammogramId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Report not found for mammogram ID: " + mammogramId));

        try {
            byte[] pdfContent = generatePdfContent(report);
            String filename = generateFilename(report);

            log.info("Successfully generated PDF report for mammogram ID: {}", mammogramId);
            return new ReportPdfResponse(
                    report.getId(),
                    filename,
                    pdfContent.length,
                    pdfContent
            );
        } catch (IOException e) {
            log.error("PDF generation failed for mammogram ID: {}", mammogramId, e);
            throw new ReportGenerationException("Failed to generate PDF report", e);
        }
    }

    private String generateFilename(Report report) {
        String patientName = report.getMammogram().getPatient().getFullName()
                .replaceAll("\\s+", "_")
                .toLowerCase();
        return String.format("mammogram_report_%s_%d.pdf",
                patientName,
                report.getId());
    }

    private byte[] generatePdfContent(Report report) throws IOException {
        try (PDDocument document = new PDDocument();
             ByteArrayOutputStream outputStream = new ByteArrayOutputStream()) {

            PDPage page = createNewPage(document);
            PDPageContentStream contentStream = new PDPageContentStream(document, page);

            try {
                float currentY = page.getMediaBox().getHeight() - MARGIN_Y;
                currentY = addDocumentHeader(contentStream, page, currentY, report);
                currentY = addPatientInformation(contentStream, currentY, report);
                currentY = addFindingsSection(contentStream, currentY, report);
                currentY = addRecommendationsSection(contentStream, currentY, report);
                addFooter(contentStream, page, report);
            } finally {
                contentStream.close();
            }

            pdfTemplateService.applyReportTemplate(
                    document,
                    report.getMammogram().getPatient().getFullName(),
                    report.getId().toString()
            );

            document.save(outputStream);
            return outputStream.toByteArray();
        }
    }

    private PDPage createNewPage(PDDocument document) {
        PDPage page = new PDPage(PDRectangle.A4);
        document.addPage(page);
        return page;
    }

    private float addDocumentHeader(PDPageContentStream contentStream, PDPage page,
                                    float currentY, Report report) throws IOException {
        // Report title
        contentStream.setFont(TITLE_FONT, TITLE_FONT_SIZE);

        String title = "MAMMOGRAM DIAGNOSTIC REPORT";
        float titleWidth = TITLE_FONT.getStringWidth(title) / 1000 * TITLE_FONT_SIZE;
        float titleX = (page.getMediaBox().getWidth() - titleWidth) / 2;

        contentStream.beginText();
        contentStream.newLineAtOffset(titleX, currentY);
        contentStream.showText(title);
        contentStream.endText();

        // Report metadata
        currentY -= LINE_HEIGHT * 2;
        contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_X, currentY);
        contentStream.showText("Report ID: " + report.getId());
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(page.getMediaBox().getWidth() - MARGIN_X - 100, currentY);
        contentStream.showText("Date: " + report.getCreatedAt().format(DATE_FORMATTER));
        contentStream.endText();

        return currentY - LINE_HEIGHT * 2;
    }

    private float addPatientInformation(PDPageContentStream contentStream,
                                        float currentY, Report report) throws IOException {
        currentY = addSectionHeader(contentStream, "PATIENT INFORMATION", currentY);

        var patient = report.getMammogram().getPatient();
        currentY = addKeyValuePair(contentStream, "Name:", patient.getFullName(), currentY);
        currentY = addKeyValuePair(contentStream, "Age:", String.valueOf(patient.getAge()), currentY);
        currentY = addKeyValuePair(contentStream, "Gender:", patient.getGender(), currentY);
        currentY = addKeyValuePair(contentStream, "Contact:", patient.getContactInfo(), currentY);

        return currentY - LINE_HEIGHT;
    }

    private float addFindingsSection(PDPageContentStream contentStream,
                                     float currentY, Report report) throws IOException {
        currentY = addSectionHeader(contentStream, "FINDINGS", currentY);
        return addWrappedText(contentStream, report.getFindings(), currentY);
    }

    private float addRecommendationsSection(PDPageContentStream contentStream,
                                            float currentY, Report report) throws IOException {
        currentY = addSectionHeader(contentStream, "RECOMMENDATIONS", currentY);
        return addWrappedText(contentStream, report.getRecommendations(), currentY);
    }

    private void addFooter(PDPageContentStream contentStream, PDPage page,
                           Report report) throws IOException {
        float footerY = MARGIN_Y;
        contentStream.setFont(FOOTER_FONT, BODY_FONT_SIZE);

        String footerText = "Generated by: " + report.getCreatedBy() + " | " +
                "Confidential Patient Report";
        float textWidth = FOOTER_FONT.getStringWidth(footerText) / 1000 * BODY_FONT_SIZE;
        float textX = (page.getMediaBox().getWidth() - textWidth) / 2;

        contentStream.beginText();
        contentStream.newLineAtOffset(textX, footerY);
        contentStream.showText(footerText);
        contentStream.endText();
    }

    private float addSectionHeader(PDPageContentStream contentStream,
                                   String text, float y) throws IOException {
        contentStream.setFont(SECTION_FONT, SECTION_FONT_SIZE);

        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_X, y);
        contentStream.showText(text);
        contentStream.endText();

        // Add underline
        contentStream.moveTo(MARGIN_X, y - 2);
        contentStream.lineTo(MARGIN_X + 150, y - 2);
        contentStream.stroke();

        return y - LINE_HEIGHT * 1.5f;
    }

    private float addKeyValuePair(PDPageContentStream contentStream,
                                  String key, String value, float y) throws IOException {
        // Key
        contentStream.setFont(BODY_BOLD_FONT, BODY_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_X, y);
        contentStream.showText(key);
        contentStream.endText();

        // Value
        contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);
        contentStream.beginText();
        contentStream.newLineAtOffset(MARGIN_X + 120, y);
        contentStream.showText(value != null ? value : "N/A");
        contentStream.endText();

        return y - LINE_HEIGHT;
    }

    private float addWrappedText(PDPageContentStream contentStream,
                                 String text, float y) throws IOException {
        if (text == null || text.isEmpty()) {
            return y - LINE_HEIGHT;
        }

        contentStream.setFont(BODY_FONT, BODY_FONT_SIZE);

        String[] paragraphs = text.split("\n");
        for (String paragraph : paragraphs) {
            String[] words = paragraph.split(" ");
            StringBuilder line = new StringBuilder();

            for (String word : words) {
                if (line.length() + word.length() > MAX_LINE_WIDTH) {
                    drawTextLine(contentStream, line.toString(), MARGIN_X, y);
                    y -= LINE_HEIGHT;
                    line = new StringBuilder(word);
                } else {
                    if (line.length() > 0) {
                        line.append(" ");
                    }
                    line.append(word);
                }
            }

            if (line.length() > 0) {
                drawTextLine(contentStream, line.toString(), MARGIN_X, y);
                y -= LINE_HEIGHT;
            }

            // Add extra space between paragraphs
            y -= LINE_HEIGHT * 0.5f;
        }

        return y;
    }

    private void drawTextLine(PDPageContentStream contentStream,
                              String text, float x, float y) throws IOException {
        contentStream.beginText();
        contentStream.newLineAtOffset(x, y);
        contentStream.showText(text);
        contentStream.endText();
    }
}