package com.diagnostic.mammogram.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class PdfTemplateService {

    private static final float FOOTER_FONT_SIZE = 8;
    private static final float HEADER_FONT_SIZE = 10;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    public void applyReportTemplate(PDDocument document, String patientName, String reportId) throws IOException {
        // Add header and footer to each page
        for (PDPage page : document.getPages()) {
            PDRectangle pageSize = page.getMediaBox();
            float pageWidth = pageSize.getWidth();

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page,
                    PDPageContentStream.AppendMode.APPEND, true, true)) {

                // Add header
                addHeader(document, contentStream, pageWidth, patientName, reportId);

                // Add footer - now passing the current page
                addFooter(document, contentStream, pageWidth, page);
            }
        }
    }

    private void addHeader(PDDocument document, PDPageContentStream contentStream, float pageWidth,
                           String patientName, String reportId) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA_BOLD, HEADER_FONT_SIZE);

        // Clinic logo (if available)
        try {
            PDImageXObject logo = PDImageXObject.createFromFile(
                    new ClassPathResource("static/images/clinic-logo.png").getFile().getAbsolutePath(),
                    document);
            contentStream.drawImage(logo, 50, 750, 100, 30);
        } catch (IOException e) {
            // Logo not found - continue without it
        }

        // Patient and report info
        float headerY = 760;
        contentStream.beginText();
        contentStream.newLineAtOffset(pageWidth - 200, headerY);
        contentStream.showText("Patient: " + patientName);
        contentStream.endText();

        contentStream.beginText();
        contentStream.newLineAtOffset(pageWidth - 200, headerY - 15);
        contentStream.showText("Report ID: " + reportId);
        contentStream.endText();

        // Horizontal line
        contentStream.moveTo(50, 740);
        contentStream.lineTo(pageWidth - 50, 740);
        contentStream.stroke();
    }

    private void addFooter(PDDocument document, PDPageContentStream contentStream, float pageWidth, PDPage currentPage) throws IOException {
        contentStream.setFont(PDType1Font.HELVETICA, FOOTER_FONT_SIZE);

        String footerText = "Confidential Medical Report - Generated on " +
                LocalDateTime.now().format(DATE_FORMATTER);
        float textWidth = PDType1Font.HELVETICA.getStringWidth(footerText) / 1000 * FOOTER_FONT_SIZE;

        // Footer text centered
        contentStream.beginText();
        contentStream.newLineAtOffset((pageWidth - textWidth) / 2, 30);
        contentStream.showText(footerText);
        contentStream.endText();

        // Horizontal line
        contentStream.moveTo(50, 50);
        contentStream.lineTo(pageWidth - 50, 50);
        contentStream.stroke();

        // Page number (using the document's page list to find current position)
        int pageNumber = document.getPages().indexOf(currentPage) + 1;
        String pageInfo = "Page " + pageNumber;
        contentStream.beginText();
        contentStream.newLineAtOffset(pageWidth - 50, 30);
        contentStream.showText(pageInfo);
        contentStream.endText();
    }

    public void applyWatermark(PDDocument document, String text) throws IOException {
        // Implementation for watermark if needed
        for (PDPage page : document.getPages()) {
            try (PDPageContentStream watermarkStream = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {

                watermarkStream.setFont(PDType1Font.HELVETICA_BOLD, 48);
                watermarkStream.setNonStrokingColor(200, 200, 200); // Light gray

                PDRectangle pageSize = page.getMediaBox();
                float x = pageSize.getWidth() / 2;
                float y = pageSize.getHeight() / 2;

                watermarkStream.beginText();
                watermarkStream.newLineAtOffset(x, y);
                watermarkStream.showText(text);
                watermarkStream.endText();
            }
        }
    }
}