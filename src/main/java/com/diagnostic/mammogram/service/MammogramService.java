package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.AIDiagnosis;
import com.diagnostic.mammogram.dto.request.MammogramUploadRequest;
import com.diagnostic.mammogram.dto.response.MammogramResponse;
import com.diagnostic.mammogram.exception.AIServiceException;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.Mammogram;
import com.diagnostic.mammogram.model.Patient;
import com.diagnostic.mammogram.repository.MammogramRepository;
import com.diagnostic.mammogram.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j; // For logging
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // For transactional operations

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service // Marks this class as a Spring service component
@RequiredArgsConstructor // Lombok: Generates constructor with all final fields
@Slf4j // Lombok: Generates a logger field
public class MammogramService {

    private final MammogramRepository mammogramRepository;
    private final PatientRepository patientRepository; // To find the patient by ID
    private final ImageStorageService imageStorageService; // For handling file storage
    private final AIIntegrationService aiIntegrationService; // For integrating with AI model

    // --- Create Operation (Upload Mammogram) ---
    @Transactional // Ensures atomicity: all operations succeed or none do
    public MammogramResponse uploadMammogram(MammogramUploadRequest uploadRequest) {
        log.info("Attempting to upload mammogram for patient ID: {}", uploadRequest.getPatientId());

        // 1. Find the Patient
        Patient patient = patientRepository.findById(uploadRequest.getPatientId())
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + uploadRequest.getPatientId()));
        log.debug("Found patient: {}", patient.getFullName());

        // 2. Store the Image File
        String imagePath;
        try {
            // imageStorageService.storeFile returns the path where the image is saved
            imagePath = imageStorageService.storeFile(uploadRequest.getImageFile(), "mammograms/" + patient.getId());
            log.debug("Image stored at: {}", imagePath);
        } catch (Exception e) {
            log.error("Failed to store mammogram image for patient {}: {}", patient.getId(), e.getMessage(), e);
            throw new RuntimeException("Failed to store mammogram image: " + e.getMessage()); // Or a more specific exception
        }

        // 3. Create and Save Mammogram Entity
        Mammogram mammogram = Mammogram.builder()
                .patient(patient)
                .imagePath(imagePath)
                .dateUploaded(LocalDateTime.now())
                .notes(uploadRequest.getNotes())
                .build();

        mammogram = mammogramRepository.save(mammogram);
        log.info("Mammogram saved with ID: {}", mammogram.getId());

        // 4. Integrate with AI Service (Optional, but highly likely for your project)
        AIDiagnosis aiDiagnosis = null;
        try {
            // Assuming AIIntegrationService takes the image path and returns a diagnosis
            aiDiagnosis = aiIntegrationService.analyzeMammogram(mammogram.getImagePath());
            log.info("AI analysis completed for mammogram ID: {}", mammogram.getId());
        } catch (AIServiceException e) {
            log.warn("AI service failed for mammogram ID {}: {}", mammogram.getId(), e.getMessage());
            // Decide how to handle AI failure: log, return partial response, or rethrow
            // For now, we'll just log and proceed without AI diagnosis in response
        } catch (Exception e) {
            log.error("Unexpected error during AI integration for mammogram ID {}: {}", mammogram.getId(), e.getMessage(), e);
        }

        // 5. Map to Response DTO
        return mapToMammogramResponse(mammogram, aiDiagnosis, patient.getFullName());
    }

    // --- Read Operations ---
    @Transactional(readOnly = true) // Optimize read operations
    public MammogramResponse getMammogramById(Long id) {
        log.info("Fetching mammogram by ID: {}", id);
        Mammogram mammogram = mammogramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram not found with ID: " + id));

        // You might want to re-run AI analysis here or store it with the mammogram model
        // For simplicity, let's assume if AI diagnosis is needed, it's either stored or rerun.
        // For this method, we'll just return the stored info for now.
        AIDiagnosis aiDiagnosis = null; // Placeholder, you might load this from a report or separate AI results table

        return mapToMammogramResponse(mammogram, aiDiagnosis, mammogram.getPatient().getFullName());
    }

    @Transactional(readOnly = true)
    public List<MammogramResponse> getAllMammograms() {
        log.info("Fetching all mammograms.");
        return mammogramRepository.findAll().stream()
                .map(mammogram -> mapToMammogramResponse(mammogram, null, mammogram.getPatient().getFullName())) // AI Diagnosis might not be available for all
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MammogramResponse> getMammogramsByPatientId(Long patientId) {
        log.info("Fetching mammograms for patient ID: {}", patientId);
        // Ensure patient exists
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with ID: " + patientId));

        return mammogramRepository.findByPatient(patient).stream()
                .map(mammogram -> mapToMammogramResponse(mammogram, null, patient.getFullName()))
                .collect(Collectors.toList());
    }

    // --- Update Operation ---
    @Transactional
    public MammogramResponse updateMammogram(Long id, String notes) {
        log.info("Updating notes for mammogram ID: {}", id);
        Mammogram mammogram = mammogramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram not found with ID: " + id));

        // Only update allowed fields. Here, just notes.
        mammogram.setNotes(notes);
        mammogram = mammogramRepository.save(mammogram);
        log.info("Mammogram ID {} notes updated.", id);

        // Re-analyze with AI if notes indicate a change or it's part of the update flow.
        // For now, we'll just return the updated notes.
        AIDiagnosis aiDiagnosis = null;
        return mapToMammogramResponse(mammogram, aiDiagnosis, mammogram.getPatient().getFullName());
    }

    // --- Delete Operation ---
    @Transactional
    public void deleteMammogram(Long id) {
        log.info("Attempting to delete mammogram ID: {}", id);
        Mammogram mammogram = mammogramRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Mammogram not found with ID: " + id));

        // 1. Delete the image file from storage
        try {
            imageStorageService.deleteFile(mammogram.getImagePath());
            log.debug("Image file deleted from storage for mammogram ID: {}", id);
        } catch (Exception e) {
            log.error("Failed to delete image file for mammogram ID {}: {}", id, e.getMessage(), e);
            // Decide whether to throw or just log and proceed with DB deletion
            // Throwing will rollback the transaction, ensuring consistency.
            throw new RuntimeException("Failed to delete associated image: " + e.getMessage());
        }

        // 2. Delete the mammogram record from the database
        mammogramRepository.delete(mammogram);
        log.info("Mammogram ID {} deleted successfully.", id);
    }

    // --- Helper Method for DTO Mapping ---
    private MammogramResponse mapToMammogramResponse(Mammogram mammogram, AIDiagnosis aiDiagnosis, String patientName) {
        return MammogramResponse.builder()
                .id(mammogram.getId())
                .patientId(mammogram.getPatient().getId())
                .patientName(patientName) // Pass patient name from service method
                .imagePath(mammogram.getImagePath())
                .dateUploaded(mammogram.getDateUploaded())
                .notes(mammogram.getNotes())
                .aiDiagnosis(aiDiagnosis) // Include AI diagnosis if available
                .build();
    }
}