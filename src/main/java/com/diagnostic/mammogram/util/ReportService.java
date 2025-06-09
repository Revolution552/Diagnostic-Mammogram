package com.diagnostic.mammogram.util;

import com.diagnostic.mammogram.dto.response.ReportPdfResponse;
import com.diagnostic.mammogram.exception.ReportGenerationException;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;

public interface ReportService {
    ReportPdfResponse generatePdfReport(Long mammogramId)
            throws ResourceNotFoundException, ReportGenerationException;
}