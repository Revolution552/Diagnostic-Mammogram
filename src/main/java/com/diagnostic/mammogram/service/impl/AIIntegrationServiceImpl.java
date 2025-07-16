package com.diagnostic.mammogram.service.impl;

import com.diagnostic.mammogram.dto.AIDiagnosis;
import com.diagnostic.mammogram.exception.AIServiceException;
import com.diagnostic.mammogram.service.AIIntegrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.io.IOException; // Added for file operations
import java.nio.file.Files; // Added for file operations
import java.nio.file.Paths; // Added for file operations
import java.time.Duration;
import java.util.Base64; // Added for Base64 encoding
import java.util.HashMap; // Added for building JSON map
import java.util.Map; // Added for building JSON map

/**
 * Concrete implementation of the AIIntegrationService using WebClient
 * to communicate with an external AI API.
 */
@Service
@Slf4j // Lombok for logging
public class AIIntegrationServiceImpl implements AIIntegrationService {

    private final WebClient webClient;

    // Inject AI service base URL from application.properties
    @Value("${ai.service.base-url}")
    private String aiServiceBaseUrl;

    // Inject AI analysis endpoint from application.properties
    // Ensure this property is set to /predict in application.properties or application.yml
    // If not set, it will default to /predict
    @Value("${ai.service.analyze-endpoint:/predict}")
    private String aiServiceAnalyzeEndpoint;

    public AIIntegrationServiceImpl(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Override
    public AIDiagnosis analyzeMammogram(String imagePath) throws AIServiceException {
        // Construct the full URL for the AI analysis endpoint
        String fullUrl = aiServiceBaseUrl + aiServiceAnalyzeEndpoint;

        // --- FIX 1: Read image file and convert to Base64 ---
        String base64Image;
        try {
            byte[] imageBytes = Files.readAllBytes(Paths.get(imagePath));
            base64Image = Base64.getEncoder().encodeToString(imageBytes);
            log.info("Successfully read image from path: {} and converted to Base64.", imagePath);
        } catch (IOException e) {
            log.error("Error reading image file from path: {}. Details: {}", imagePath, e.getMessage(), e);
            throw new AIServiceException("Failed to read image file for AI analysis: " + e.getMessage(), e);
        }

        // --- FIX 2: Define the request body to send Base64 image under "image" key ---
        Map<String, String> requestBodyMap = new HashMap<>();
        requestBodyMap.put("image", base64Image); // Key must be "image" as expected by Python Flask app

        log.info("Sending mammogram image (Base64) to AI service at: {}", fullUrl);

        try {
            // Perform the HTTP POST request to the AI service
            AIDiagnosis aiDiagnosis = webClient.post()
                    .uri(fullUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBodyMap) // Send the map as JSON
                    .retrieve() // Start retrieving response
                    .onStatus(status -> status.is4xxClientError(), response -> {
                        log.error("AI service client error: {}", response.statusCode());
                        return response.bodyToMono(String.class)
                                .map(body -> new AIServiceException("AI Service Client Error (" + response.statusCode() + "): " + body))
                                .flatMap(Mono::error);
                    })
                    .onStatus(status -> status.is5xxServerError(), response -> {
                        log.error("AI service server error: {}", response.statusCode());
                        return response.bodyToMono(String.class)
                                .map(body -> new AIServiceException("AI Service Server Error (" + response.statusCode() + "): " + body))
                                .flatMap(Mono::error);
                    })
                    .bodyToMono(AIDiagnosis.class) // Convert response body to AIDiagnosis DTO
                    .timeout(Duration.ofSeconds(60)) // Set a timeout for the AI analysis
                    .block(); // Block and wait for the response (consider using reactive approach in controller if feasible)

            if (aiDiagnosis == null) {
                log.error("AI service returned null diagnosis for imagePath: {}", imagePath);
                throw new AIServiceException("AI service returned an empty or invalid diagnosis.");
            }

            log.info("AI analysis successful for imagePath: {}. Diagnosis: {}", imagePath, aiDiagnosis.getDiagnosisSummary());
            return aiDiagnosis;

        } catch (WebClientResponseException e) {
            log.error("WebClient error during AI analysis for imagePath {}: {} - {}", imagePath, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new AIServiceException("Failed to get AI diagnosis due to WebClient error: " + e.getResponseBodyAsString(), e);
        } catch (AIServiceException e) {
            throw e; // Re-throw our specific AI service exception already created in onStatus or by us
        } catch (Exception e) {
            log.error("An unexpected error occurred during AI analysis for imagePath {}: {}", imagePath, e.getMessage(), e);
            throw new AIServiceException("An unexpected error occurred during AI analysis: " + e.getMessage(), e);
        }
    }
}
