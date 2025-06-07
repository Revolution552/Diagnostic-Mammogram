package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.AIDiagnosis;
import com.diagnostic.mammogram.exception.AIServiceException;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.Mammogram;
import com.diagnostic.mammogram.repository.MammogramRepository;
import com.diagnostic.mammogram.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;  // ✅ Correct import
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AIIntegrationService {

    private final RestTemplate restTemplate;
    private final ImageStorageService imageStorageService;
    private final MammogramRepository mammogramRepository;

    @Value("${ai.service.url}")  // Now correctly using Spring's @Value
    private String aiServiceUrl;

    public AIDiagnosis analyzeMammogram(Long mammogramId) {
        Mammogram mammogram = mammogramRepository.findById(mammogramId)
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram not found with ID: " + mammogramId));

        String imageUrl = imageStorageService.getImageUrl(mammogram.getImagePath());
        if (imageUrl == null || imageUrl.isBlank()) {
            throw new AIServiceException("Image URL is not available for mammogram ID: " + mammogramId);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> requestBody = new HashMap<>();
        requestBody.put("image_url", imageUrl);

        HttpEntity<Map<String, String>> request = new HttpEntity<>(requestBody, headers);

        try {
            ResponseEntity<AIDiagnosis> response = restTemplate.postForEntity(
                    aiServiceUrl + "/analyze",
                    request,
                    AIDiagnosis.class
            );

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                throw new AIServiceException("AI service returned invalid response for mammogram ID: " + mammogramId);
            }

            return response.getBody();
        } catch (RestClientException e) {
            throw new AIServiceException("Failed to communicate with AI service for mammogram ID: " + mammogramId, e);
        }
    }
}