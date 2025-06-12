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

import java.time.Duration; // For timeout configuration

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
    @Value("${ai.service.analyze-endpoint:/analyze-mammogram}")
    private String aiServiceAnalyzeEndpoint;

    public AIIntegrationServiceImpl(WebClient.Builder webClientBuilder) {
        // Build WebClient. Consider using .baseUrl(aiServiceBaseUrl) here if it's constant
        // for all calls to this AI service. For flexibility, we'll set it per call.
        this.webClient = webClientBuilder.build();
    }

    @Override
    public AIDiagnosis analyzeMammogram(String imagePath) throws AIServiceException {
        // Construct the full URL for the AI analysis endpoint
        String fullUrl = aiServiceBaseUrl + aiServiceAnalyzeEndpoint;
        log.info("Sending mammogram image path to AI service: {} at {}", imagePath, fullUrl);

        // Define the request body for the AI service.
        // This is a simple JSON object, you might need a more complex DTO based on your AI API.
        // For example: { "imageUrl": "...", "patientId": "..." }
        String requestBody = String.format("{\"imagePath\": \"%s\"}", imagePath);

        try {
            // Perform the HTTP POST request to the AI service
            AIDiagnosis aiDiagnosis = webClient.post()
                    .uri(fullUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve() // Start retrieving response
                    // Fix 1: Use lambda for HttpStatus methods
                    .onStatus(status -> status.is4xxClientError(), response -> {
                        log.error("AI service client error: {}", response.statusCode());
                        // Fix 2: Map to AIServiceException and then flatMap to Mono.error
                        return response.bodyToMono(String.class)
                                .map(body -> new AIServiceException("AI Service Client Error (" + response.statusCode() + "): " + body))
                                .flatMap(Mono::error); // This explicitly converts to Mono<? extends Throwable>
                    })
                    // Fix 1: Use lambda for HttpStatus methods
                    .onStatus(status -> status.is5xxServerError(), response -> {
                        log.error("AI service server error: {}", response.statusCode());
                        // Fix 2: Map to AIServiceException and then flatMap to Mono.error
                        return response.bodyToMono(String.class)
                                .map(body -> new AIServiceException("AI Service Server Error (" + response.statusCode() + "): " + body))
                                .flatMap(Mono::error); // This explicitly converts to Mono<? extends Throwable>
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
            // This catch block handles exceptions where WebClient already threw a specific error (e.g., non-2xx status not handled by onStatus)
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