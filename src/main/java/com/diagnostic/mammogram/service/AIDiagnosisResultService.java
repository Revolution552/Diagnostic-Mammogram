package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.AIDiagnosis; // Import the DTO for mapping
import com.diagnostic.mammogram.dto.request.AIDiagnosisUpdateRequest; // NEW: Import the update request DTO
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.AIDiagnosisResult;
import com.diagnostic.mammogram.model.Mammogram; // Needed to find by mammogram
import com.diagnostic.mammogram.model.Patient; // NEW: Import Patient model
import com.diagnostic.mammogram.repository.AIDiagnosisResultRepository;
import com.diagnostic.mammogram.repository.MammogramRepository; // Needed to find Mammogram for linking
import com.diagnostic.mammogram.repository.PatientRepository; // NEW: Import PatientRepository
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AIDiagnosisResultService {

    private final AIDiagnosisResultRepository aiDiagnosisResultRepository;
    private final MammogramRepository mammogramRepository; // To find Mammogram when needed
    private final PatientRepository patientRepository; // NEW: Inject PatientRepository

    /**
     * Retrieves an AI diagnosis result by its unique ID.
     *
     * @param id The ID of the AI diagnosis result.
     * @return The AIDiagnosis DTO.
     * @throws ResourceNotFoundException if the result is not found.
     */
    @Transactional(readOnly = true)
    public AIDiagnosis getAIDiagnosisResultById(Long id) {
        log.info("Fetching AI Diagnosis Result by ID: {}", id);
        AIDiagnosisResult result = aiDiagnosisResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AI Diagnosis Result not found with ID: " + id));
        return mapToAIDiagnosisDto(result);
    }

    /**
     * Retrieves an AI diagnosis result associated with a specific mammogram ID.
     *
     * @param mammogramId The ID of the mammogram.
     * @return The AIDiagnosis DTO.
     * @throws ResourceNotFoundException if the mammogram or its associated AI result is not found.
     */
    @Transactional(readOnly = true)
    public AIDiagnosis getAIDiagnosisResultByMammogramId(Long mammogramId) {
        log.info("Fetching AI Diagnosis Result for Mammogram ID: {}", mammogramId);
        Mammogram mammogram = mammogramRepository.findById(mammogramId)
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram not found with ID: " + mammogramId));

        AIDiagnosisResult result = aiDiagnosisResultRepository.findByMammogram(mammogram)
                .orElseThrow(() -> new ResourceNotFoundException("AI Diagnosis Result not found for Mammogram ID: " + mammogramId));
        return mapToAIDiagnosisDto(result);
    }

    /**
     * Retrieves all AI diagnosis results.
     *
     * @return A list of AIDiagnosis DTOs.
     */
    @Transactional(readOnly = true)
    public List<AIDiagnosis> getAllAIDiagnosisResults() {
        log.info("Fetching all AI Diagnosis Results.");
        return aiDiagnosisResultRepository.findAll().stream()
                .map(this::mapToAIDiagnosisDto)
                .collect(Collectors.toList());
    }

    /**
     * NEW: Retrieves all AI diagnosis results for a specific patient ID.
     *
     * @param patientId The ID of the patient.
     * @return A list of AIDiagnosis DTOs for the specified patient.
     */
    @Transactional(readOnly = true)
    public List<AIDiagnosis> getAIDiagnosisResultsByPatientId(Long patientId) {
        log.info("Fetching AI Diagnosis Results for Patient ID: {}", patientId);
        return aiDiagnosisResultRepository.findByPatientId(patientId).stream()
                .map(this::mapToAIDiagnosisDto)
                .collect(Collectors.toList());
    }

    /**
     * NEW: Retrieves all AI diagnosis results for a specific patient name.
     *
     * @param patientName The full name (or part of it) of the patient.
     * @return A list of AIDiagnosis DTOs for the specified patient.
     * @throws ResourceNotFoundException if no patient is found with the given name.
     */
    @Transactional(readOnly = true)
    public List<AIDiagnosis> getAIDiagnosisResultsByPatientName(String patientName) {
        log.info("Fetching AI Diagnosis Results for Patient Name: {}", patientName);

        // Find patient(s) by name. Using findByFullNameContainingIgnoreCase for partial matches.
        List<Patient> patients = patientRepository.findByFullNameContainingIgnoreCase(patientName);

        if (patients.isEmpty()) {
            throw new ResourceNotFoundException("No patient found with name containing: " + patientName);
        }

        // Collect all AI diagnoses for all found patients
        return patients.stream()
                .flatMap(patient -> aiDiagnosisResultRepository.findByPatientId(patient.getId()).stream())
                .map(this::mapToAIDiagnosisDto)
                .collect(Collectors.toList());
    }

    /**
     * Updates an existing AI diagnosis result using individual fields.
     * This might be used if a radiologist manually corrects or adds to the AI's findings/recommendations.
     *
     * @param id The ID of the AI diagnosis result to update.
     * @param updatedPrediction The new prediction string (optional).
     * @param updatedDetailedFindings The new detailed findings (optional).
     * @param updatedRecommendation The new recommendation (optional).
     * @return The updated AIDiagnosis DTO.
     * @throws ResourceNotFoundException if the result is not found.
     */
    @Transactional
    public AIDiagnosis updateAIDiagnosisResult(Long id,
                                               String updatedPrediction,
                                               String updatedDetailedFindings,
                                               String updatedRecommendation) {
        log.info("Updating AI Diagnosis Result ID: {}", id);
        AIDiagnosisResult result = aiDiagnosisResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AI Diagnosis Result not found with ID: " + id));

        Optional.ofNullable(updatedPrediction).ifPresent(result::setPrediction);
        Optional.ofNullable(updatedDetailedFindings).ifPresent(result::setDetailedFindings);
        Optional.ofNullable(updatedRecommendation).ifPresent(result::setRecommendation);

        // Note: confidenceScore and probabilities are typically not updated manually
        // as they come directly from the AI model. If needed, you would re-run AI analysis.

        AIDiagnosisResult savedResult = aiDiagnosisResultRepository.save(result);
        log.info("AI Diagnosis Result ID {} updated.", id);
        return mapToAIDiagnosisDto(savedResult);
    }

    /**
     * NEW: Updates an existing AI diagnosis result using a request DTO.
     * This provides a more structured way to handle updates from API requests.
     *
     * @param id The ID of the AI diagnosis result to update.
     * @param request The AIDiagnosisUpdateRequest DTO containing fields to update.
     * @return The updated AIDiagnosis DTO.
     * @throws ResourceNotFoundException if the result is not found.
     */
    @Transactional
    public AIDiagnosis updateAIDiagnosisResult(Long id, AIDiagnosisUpdateRequest request) {
        log.info("Updating AI Diagnosis Result ID: {} with DTO request.", id);
        AIDiagnosisResult result = aiDiagnosisResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AI Diagnosis Result not found with ID: " + id));

        Optional.ofNullable(request.getPrediction()).ifPresent(result::setPrediction);
        Optional.ofNullable(request.getDetailedFindings()).ifPresent(result::setDetailedFindings);
        Optional.ofNullable(request.getRecommendation()).ifPresent(result::setRecommendation);

        AIDiagnosisResult savedResult = aiDiagnosisResultRepository.save(result);
        log.info("AI Diagnosis Result ID {} updated via DTO.", id);
        return mapToAIDiagnosisDto(savedResult);
    }


    /**
     * Deletes an AI diagnosis result by its ID.
     * Note: Deleting a Mammogram with CascadeType.ALL will also delete its associated AIDiagnosisResult.
     * This method is for direct deletion of the AI result if needed.
     *
     * @param id The ID of the AI diagnosis result to delete.
     * @throws ResourceNotFoundException if the result is not found.
     */
    @Transactional
    public void deleteAIDiagnosisResult(Long id) {
        log.info("Attempting to delete AI Diagnosis Result ID: {}", id);
        AIDiagnosisResult result = aiDiagnosisResultRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AI Diagnosis Result not found with ID: " + id));
        aiDiagnosisResultRepository.delete(result);
        log.info("AI Diagnosis Result ID {} deleted successfully.", id);
    }

    /**
     * Helper method to map AIDiagnosisResult entity to AIDiagnosis DTO.
     * @param entity The AIDiagnosisResult entity.
     * @return The corresponding AIDiagnosis DTO.
     */
    private AIDiagnosis mapToAIDiagnosisDto(AIDiagnosisResult entity) {
        return AIDiagnosis.builder()
                .prediction(entity.getPrediction())
                .probabilities(entity.getProbabilitiesList()) // Use the transient list
                .confidenceScore(entity.getConfidenceScore()) // Map confidence score
                .diagnosisSummary(entity.getDetailedFindings()) // Map detailed findings to diagnosis summary
                .detailedFindings(entity.getDetailedFindings()) // NEW: Map detailedFindings
                .recommendation(entity.getRecommendation()) // NEW: Map recommendation
                .build();
    }
}
