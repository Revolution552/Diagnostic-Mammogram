package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.AIDiagnosis;
import com.diagnostic.mammogram.dto.request.MammogramUploadRequest;
import com.diagnostic.mammogram.dto.response.MammogramResponse;
import com.diagnostic.mammogram.exception.AIServiceException;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.AIDiagnosisResult;
import com.diagnostic.mammogram.model.Mammogram;
import com.diagnostic.mammogram.model.Patient;
import com.diagnostic.mammogram.repository.AIDiagnosisResultRepository;
import com.diagnostic.mammogram.repository.MammogramRepository;
import com.diagnostic.mammogram.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MammogramService {

    private final MammogramRepository mammogramRepository;
    private final PatientRepository patientRepository;
    private final ImageStorageService imageStorageService;
    private final AIIntegrationService aiIntegrationService;
    private final AIDiagnosisResultRepository aiDiagnosisResultRepository;

    // --- Create Operation (Upload Mammogram) ---
    @Transactional
    public MammogramResponse uploadMammogram(MammogramUploadRequest uploadRequest) {
        log.info("Attempting to upload mammogram for patient ID: {}", uploadRequest.getPatientId());

        Patient patient = patientRepository.findById(uploadRequest.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + uploadRequest.getPatientId()));
        log.debug("Found patient: {}", patient.getFullName());

        String relativeImagePath;
        try {
            relativeImagePath = imageStorageService.storeFile(uploadRequest.getImageFile(), "mammograms/" + patient.getId());
            log.debug("Image stored at relative path: {}", relativeImagePath);
        } catch (Exception e) {
            log.error("Failed to store mammogram image for patient {}: {}", patient.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to store mammogram image: " + e.getMessage());
        }

        Mammogram mammogram = Mammogram.builder()
                .patient(patient)
                .imagePath(relativeImagePath)
                .dateUploaded(LocalDateTime.now())
                .notes(uploadRequest.getNotes())
                .build();

        // Save mammogram first to get an ID for linking AI diagnosis
        mammogram = mammogramRepository.save(mammogram);
        log.info("Mammogram saved with ID: {}", mammogram.getId());

        // 4. Integrate with AI Service and Save Result
        AIDiagnosis aiDiagnosisDto = null;
        AIDiagnosisResult aiDiagnosisResult = null;
        try {
            String absoluteImagePathForAI = imageStorageService.getAbsoluteFilePath(mammogram.getImagePath());
            log.info("Passing absolute image path to AI service: {}", absoluteImagePathForAI);

            aiDiagnosisDto = aiIntegrationService.analyzeMammogram(absoluteImagePathForAI);
            log.info("AI analysis completed for mammogram ID: {}. Diagnosis: {}", mammogram.getId(), aiDiagnosisDto.getDiagnosisSummary());

            // Create and save AIDiagnosisResult entity
            aiDiagnosisResult = AIDiagnosisResult.builder()
                    .mammogram(mammogram) // Link to the newly saved mammogram
                    .patientId(patient.getId())
                    .prediction(aiDiagnosisDto.getPrediction())
                    .probabilitiesList(aiDiagnosisDto.getProbabilities())
                    .confidenceScore(aiDiagnosisDto.getConfidenceScore())
                    .detailedFindings(aiDiagnosisDto.getDetailedFindings()) // Use detailedFindings from DTO
                    .recommendation(aiDiagnosisDto.getRecommendation()) // Use recommendation from DTO
                    .analysisDate(LocalDateTime.now())
                    .build();

            aiDiagnosisResult = aiDiagnosisResultRepository.save(aiDiagnosisResult);
            log.info("AI Diagnosis Result saved with ID: {} for mammogram ID: {}", aiDiagnosisResult.getId(), mammogram.getId());

            // Link the AIDiagnosisResult back to the Mammogram entity
            mammogram.setAiDiagnosisResult(aiDiagnosisResult);
            mammogramRepository.save(mammogram); // Save mammogram again to update the link
            log.info("Mammogram ID {} linked with AI Diagnosis Result ID {}", mammogram.getId(), aiDiagnosisResult.getId());

        } catch (AIServiceException e) {
            log.warn("AI service failed for mammogram ID {}: {}", mammogram.getId(), e.getMessage());
        } catch (Exception e) {
            log.error("Unexpected error during AI integration or saving AI result for mammogram ID {}: {}", mammogram.getId(), e.getMessage(), e);
        }

        // 5. Map to Response DTO
        return mapToMammogramResponse(mammogram, aiDiagnosisDto, patient.getFullName());
    }

    // --- Read Operations ---
    @Transactional(readOnly = true)
    public MammogramResponse getMammogramById(Long id) {
        log.info("Fetching mammogram by ID: {}", id);
        Mammogram mammogram = mammogramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram not found with ID: " + id));

        AIDiagnosis aiDiagnosisDto = null;
        if (mammogram.getAiDiagnosisResult() != null) {
            AIDiagnosisResult storedResult = mammogram.getAiDiagnosisResult();
            aiDiagnosisDto = AIDiagnosis.builder()
                    .prediction(storedResult.getPrediction())
                    .probabilities(storedResult.getProbabilitiesList())
                    .diagnosisSummary(storedResult.getDetailedFindings())
                    .confidenceScore(storedResult.getConfidenceScore())
                    .detailedFindings(storedResult.getDetailedFindings())
                    .recommendation(storedResult.getRecommendation())
                    .build();
            log.debug("Retrieved AI Diagnosis Result for mammogram ID {}: {}", id, aiDiagnosisDto.getDiagnosisSummary());
        } else {
            log.debug("No AI Diagnosis Result found for mammogram ID: {}", id);
        }

        return mapToMammogramResponse(mammogram, aiDiagnosisDto, mammogram.getPatient().getFullName());
    }

    @Transactional(readOnly = true)
    public List<MammogramResponse> getAllMammograms() {
        log.info("Fetching all mammograms.");
        return mammogramRepository.findAll().stream()
                .map(mammogram -> {
                    AIDiagnosis aiDiagnosisDto = null;
                    if (mammogram.getAiDiagnosisResult() != null) {
                        AIDiagnosisResult storedResult = mammogram.getAiDiagnosisResult();
                        aiDiagnosisDto = AIDiagnosis.builder()
                                .prediction(storedResult.getPrediction())
                                .probabilities(storedResult.getProbabilitiesList())
                                .diagnosisSummary(storedResult.getDetailedFindings())
                                .confidenceScore(storedResult.getConfidenceScore())
                                .detailedFindings(storedResult.getDetailedFindings())
                                .recommendation(storedResult.getRecommendation())
                                .build();
                    }
                    return mapToMammogramResponse(mammogram, aiDiagnosisDto, mammogram.getPatient().getFullName());
                })
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MammogramResponse> getMammogramsByPatientId(Long patientId) {
        log.info("Fetching mammograms for patient ID: {}", patientId);
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        return mammogramRepository.findByPatient(patient).stream()
                .map(mammogram -> {
                    AIDiagnosis aiDiagnosisDto = null;
                    if (mammogram.getAiDiagnosisResult() != null) {
                        AIDiagnosisResult storedResult = mammogram.getAiDiagnosisResult();
                        aiDiagnosisDto = AIDiagnosis.builder()
                                .prediction(storedResult.getPrediction())
                                .probabilities(storedResult.getProbabilitiesList())
                                .diagnosisSummary(storedResult.getDetailedFindings())
                                .confidenceScore(storedResult.getConfidenceScore())
                                .detailedFindings(storedResult.getDetailedFindings())
                                .recommendation(storedResult.getRecommendation())
                                .build();
                    }
                    return mapToMammogramResponse(mammogram, aiDiagnosisDto, patient.getFullName());
                })
                .collect(Collectors.toList());
    }

    // --- Update Operation ---
    @Transactional
    public MammogramResponse updateMammogram(Long id, String notes) {
        log.info("Updating notes for mammogram ID: {}", id);
        Mammogram mammogram = mammogramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram not found with ID: " + id));

        mammogram.setNotes(notes);
        mammogram = mammogramRepository.save(mammogram);
        log.info("Mammogram ID {} notes updated.", id);

        AIDiagnosis aiDiagnosisDto = null;
        if (mammogram.getAiDiagnosisResult() != null) {
            AIDiagnosisResult storedResult = mammogram.getAiDiagnosisResult();
            aiDiagnosisDto = AIDiagnosis.builder()
                    .prediction(storedResult.getPrediction())
                    .probabilities(storedResult.getProbabilitiesList())
                    .diagnosisSummary(storedResult.getDetailedFindings())
                    .confidenceScore(storedResult.getConfidenceScore())
                    .detailedFindings(storedResult.getDetailedFindings())
                    .recommendation(storedResult.getRecommendation())
                    .build();
        }
        return mapToMammogramResponse(mammogram, aiDiagnosisDto, mammogram.getPatient().getFullName());
    }

    // --- Delete Operation ---
    @Transactional
    public void deleteMammogram(Long id) {
        log.info("Attempting to delete mammogram ID: {}", id);
        Mammogram mammogram = mammogramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram not found with ID: " + id));

        // 1. Delete the image file from storage
        try {
            String absoluteImagePathForDeletion = imageStorageService.getAbsoluteFilePath(mammogram.getImagePath());
            imageStorageService.deleteFile(absoluteImagePathForDeletion);
            log.debug("Image file deleted from storage for mammogram ID: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete image file for mammogram ID {}: {}", id, e.getMessage(), e);
            throw new RuntimeException("Failed to delete associated image: " + e.getMessage());
        }

        // 2. Deleting Mammogram will cascade delete AIDiagnosisResult due to CascadeType.ALL
        mammogramRepository.delete(mammogram);
        log.info("Mammogram ID {} deleted successfully.", id);
    }

    // --- Helper Method for DTO Mapping ---
    private MammogramResponse mapToMammogramResponse(Mammogram mammogram, AIDiagnosis aiDiagnosis, String patientName) {
        String imageUrl = imageStorageService.getFileUrl(mammogram.getImagePath());

        // Convert AIDiagnosis DTO to MammogramResponse.AiDiagnosisResponse nested DTO
        MammogramResponse.AiDiagnosisResponse aiResponseForMammogramResponse = null;
        if (aiDiagnosis != null) {
            aiResponseForMammogramResponse = MammogramResponse.AiDiagnosisResponse.builder()
                    .diagnosisSummary(aiDiagnosis.getDiagnosisSummary())
                    .prediction(aiDiagnosis.getPrediction())
                    .confidenceScore(aiDiagnosis.getConfidenceScore())
                    .probabilities(aiDiagnosis.getProbabilities())
                    .detailedFindings(aiDiagnosis.getDetailedFindings())
                    .recommendation(aiDiagnosis.getRecommendation())
                    .build();
        }

        return MammogramResponse.builder()
                .id(mammogram.getId())
                .patientId(mammogram.getPatient().getId())
                .patientName(patientName)
                .imagePath(imageUrl)
                .dateUploaded(mammogram.getDateUploaded())
                .notes(mammogram.getNotes())
                .aiDiagnosis(aiResponseForMammogramResponse) // Use the converted nested DTO
                // Populate reportId. This assumes Mammogram entity has a way to get reportId,
                // or it's null if no report is linked yet.
                // If your Mammogram entity has a 'report' relationship, you'd fetch mammogram.getReport().getId()
                .reportId(null) // Set to null for now, as no direct reportId is available on Mammogram entity
                .build();
    }
}
