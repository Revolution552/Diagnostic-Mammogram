package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.AIDiagnosis;
import com.diagnostic.mammogram.dto.request.AIDiagnosisUpdateRequest; // Import the update request DTO
import com.diagnostic.mammogram.service.AIDiagnosisResultService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid; // For @Valid annotation

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController // Marks this class as a Spring REST Controller
@RequestMapping("/api/ai-diagnoses") // Base path for all endpoints in this controller
@RequiredArgsConstructor // Lombok: Generates constructor with all final fields
@Slf4j // Lombok: Generates a logger field
public class AIDiagnosisResultController {

    private final AIDiagnosisResultService aiDiagnosisResultService; // Inject the new service

    /**
     * Retrieves an AI diagnosis result by its unique ID.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     *
     * @param id The ID of the AI diagnosis result to retrieve.
     * @return ResponseEntity with a Map representing the response and HTTP status 200 (OK).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> getAIDiagnosisResultById(@PathVariable Long id) {
        log.info("Received request to get AI Diagnosis Result by ID: {}", id);
        AIDiagnosis responseData = aiDiagnosisResultService.getAIDiagnosisResultById(id);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "AI Diagnosis Result retrieved successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Retrieves an AI diagnosis result associated with a specific mammogram ID.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     *
     * @param mammogramId The ID of the mammogram.
     * @return ResponseEntity with a Map representing the response and HTTP status 200 (OK).
     */
    @GetMapping("/by-mammogram/{mammogramId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> getAIDiagnosisResultByMammogramId(@PathVariable Long mammogramId) {
        log.info("Received request to get AI Diagnosis Result for Mammogram ID: {}", mammogramId);
        AIDiagnosis responseData = aiDiagnosisResultService.getAIDiagnosisResultByMammogramId(mammogramId);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "AI Diagnosis Result for mammogram retrieved successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Retrieves all AI diagnosis results.
     * Accessible by ADMIN, RADIOLOGIST.
     *
     * @return ResponseEntity with a Map representing the response and HTTP status 200 (OK).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')") // Changed to include DOCTOR for broader access
    public ResponseEntity<Map<String, Object>> getAllAIDiagnosisResults() {
        log.info("Received request to get all AI Diagnosis Results.");
        List<AIDiagnosis> responseData = aiDiagnosisResultService.getAllAIDiagnosisResults();

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "All AI Diagnosis Results retrieved successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * NEW: Retrieves AI diagnosis results for a specific patient name.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     *
     * @param patientName The full name (or part of it) of the patient.
     * @return ResponseEntity with a Map representing the response and HTTP status 200 (OK).
     */
    @GetMapping("/by-patient-name/{patientName}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> getAIDiagnosisResultsByPatientName(@PathVariable String patientName) {
        log.info("Received request to get AI Diagnosis Results for Patient Name: {}", patientName);
        List<AIDiagnosis> responseData = aiDiagnosisResultService.getAIDiagnosisResultsByPatientName(patientName);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "AI Diagnosis Results for patient name retrieved successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Retrieves all AI diagnosis results for a specific patient ID.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     *
     * @param patientId The ID of the patient.
     * @return ResponseEntity with a Map representing the response and HTTP status 200 (OK).
     */
    @GetMapping("/by-patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> getAIDiagnosisResultsByPatientId(@PathVariable Long patientId) {
        log.info("Received request to get AI Diagnosis Results for Patient ID: {}", patientId);
        List<AIDiagnosis> responseData = aiDiagnosisResultService.getAIDiagnosisResultsByPatientId(patientId);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "AI Diagnosis Results for patient ID retrieved successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * NEW: Updates an existing AI diagnosis result.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     * Allows updating fields like detailed findings or recommendations.
     *
     * @param id The ID of the AI diagnosis result to update.
     * @param request The AIDiagnosisUpdateRequest DTO containing the fields to update.
     * @return ResponseEntity with the updated AIDiagnosis DTO.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> updateAIDiagnosisResult(
            @PathVariable Long id,
            @Valid @RequestBody AIDiagnosisUpdateRequest request) { // @Valid for DTO validation
        log.info("Received request to update AI Diagnosis Result ID: {}", id);
        AIDiagnosis updatedData = aiDiagnosisResultService.updateAIDiagnosisResult(id, request);

        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "AI Diagnosis Result updated successfully.");
        responseMap.put("data", updatedData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * NEW: Retrieves AI diagnosis results filtered by prediction type.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     *
     * @param prediction The prediction type to filter by (e.g., "NORMAL", "ABNORMAL").
     * @return ResponseEntity with a list of AIDiagnosis DTOs matching the prediction.
     */
    @GetMapping("/by-prediction/{prediction}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> getAIDiagnosisResultsByPrediction(@PathVariable String prediction) {
        log.info("Received request to get AI Diagnosis Results by Prediction: {}", prediction);
        // You would need to add a findByPrediction method to AIDiagnosisResultRepository and AIDiagnosisResultService
        // For now, this assumes such a method exists or you'll implement it.
        // Example: List<AIDiagnosis> responseData = aiDiagnosisResultService.getAIDiagnosisResultsByPrediction(prediction);
        // As a placeholder, let's just return all for now or throw an error if not implemented in service.
        // For actual implementation, you'd add:
        // 1. In AIDiagnosisResultRepository: List<AIDiagnosisResult> findByPrediction(String prediction);
        // 2. In AIDiagnosisResultService: public List<AIDiagnosis> getAIDiagnosisResultsByPrediction(String prediction) { ... }
        // For now, let's assume it's implemented and call it.
        List<AIDiagnosis> responseData = aiDiagnosisResultService.getAllAIDiagnosisResults().stream()
                .filter(d -> d.getPrediction() != null && d.getPrediction().equalsIgnoreCase(prediction))
                .collect(Collectors.toList());


        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "AI Diagnosis Results filtered by prediction retrieved successfully.");
        responseMap.put("data", responseData);
        responseMap.put("count", responseData.size());
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }
}
