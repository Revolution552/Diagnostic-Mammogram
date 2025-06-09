package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.Mammogram;
import com.diagnostic.mammogram.model.Patient;
import com.diagnostic.mammogram.repository.MammogramRepository;
import com.diagnostic.mammogram.repository.PatientRepository;
import com.diagnostic.mammogram.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/mammograms")
@RequiredArgsConstructor
public class MammogramController {

    private static final Logger logger = LoggerFactory.getLogger(MammogramController.class);

    private final MammogramRepository mammogramRepository;
    private final PatientRepository patientRepository;
    private final ImageStorageService imageStorageService;

    @PostMapping("/upload/{patientId}")
    public ResponseEntity<Map<String, Object>> uploadMammogram(
            @PathVariable Long patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String notes) {

        Map<String, Object> response = new HashMap<>();

        try {
            logger.info("Starting mammogram upload for patient ID: {}", patientId);
            logger.debug("File details - Name: {}, Size: {} bytes", file.getOriginalFilename(), file.getSize());

            // Validate file input
            if (file.isEmpty()) {
                throw new IllegalArgumentException("File cannot be empty");
            }

            Patient patient = patientRepository.findById(patientId)
                    .orElseThrow(() -> {
                        String errorMsg = "Patient not found with ID: " + patientId;
                        logger.error(errorMsg);
                        return new ResourceNotFoundException(errorMsg);
                    });

            String imagePath = imageStorageService.storeImage(file);
            logger.info("Image successfully stored at path: {}", imagePath);

            Mammogram mammogram = new Mammogram();
            mammogram.setPatient(patient);
            mammogram.setImagePath(imagePath);
            mammogram.setUploadDate(new Date());
            mammogram.setNotes(notes);

            Mammogram savedMammogram = mammogramRepository.save(mammogram);
            logger.info("Mammogram record successfully saved with ID: {}", savedMammogram.getId());

            // Create a response DTO to avoid lazy loading issues
            Map<String, Object> mammogramResponse = new HashMap<>();
            mammogramResponse.put("id", savedMammogram.getId());
            mammogramResponse.put("imagePath", savedMammogram.getImagePath());
            mammogramResponse.put("uploadDate", savedMammogram.getUploadDate());
            mammogramResponse.put("notes", savedMammogram.getNotes());

            // Include only necessary patient details
            Map<String, Object> patientResponse = new HashMap<>();
            patientResponse.put("id", patient.getId());
            patientResponse.put("name", patient.getName());
            mammogramResponse.put("patient", patientResponse);

            response.put("status", "success");
            response.put("message", "Mammogram uploaded successfully");
            response.put("data", mammogramResponse);

            return ResponseEntity.ok(response);

        } catch (ResourceNotFoundException ex) {
            logger.error("Resource not found error: {}", ex.getMessage());
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (IllegalArgumentException ex) {
            logger.error("Validation error: {}", ex.getMessage());
            response.put("status", "error");
            response.put("message", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception ex) {
            logger.error("Error occurred during mammogram upload: {}", ex.getMessage(), ex);
            response.put("status", "error");
            response.put("message", "An error occurred while uploading the mammogram");
            response.put("details", ex.getMessage()); // Include specific error for debugging
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}