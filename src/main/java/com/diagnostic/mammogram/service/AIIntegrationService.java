package com.diagnostic.mammogram.service;


import com.diagnostic.mammogram.dto.AIDiagnosis;
import com.diagnostic.mammogram.exception.AIServiceException;

/**
 * Interface for integrating with an external Artificial Intelligence (AI) service
 * for mammogram analysis.
 */
public interface AIIntegrationService {

    /**
     * Sends a mammogram image path (or reference) to the AI service for analysis
     * and retrieves the diagnostic results.
     *
     * @param imagePath The path or identifier of the mammogram image to analyze.
     * This could be a local file path, a cloud storage URL (e.g., S3 URL),
     * or an ID to retrieve the image.
     * @return AIDiagnosis object containing the AI's findings and recommendations.
     * @throws AIServiceException if there's an issue communicating with the AI service
     * or if the AI service returns an error.
     */
    AIDiagnosis analyzeMammogram(String imagePath) throws AIServiceException;

    // You might add other methods later, e.g.:
    // boolean isAIServiceHealthy();
    // AIDiagnosis analyzeMammogram(byte[] imageData); // If sending raw bytes
}