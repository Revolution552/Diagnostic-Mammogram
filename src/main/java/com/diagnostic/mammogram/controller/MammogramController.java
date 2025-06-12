package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.MammogramUploadRequest;
import com.diagnostic.mammogram.dto.response.MammogramResponse;
import com.diagnostic.mammogram.service.MammogramService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController // Marks this class as a Spring REST Controller
@RequestMapping("/api/mammograms") // Base path for all endpoints in this controller
@RequiredArgsConstructor // Lombok: Generates constructor with all final fields
@Slf4j // Lombok: Generates a logger field
public class MammogramController {

    private final MammogramService mammogramService;

    /**
     * Uploads a new mammogram image and processes it.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     *
     * @param patientId The ID of the patient associated with the mammogram.
     * @param imageFile The mammogram image file.
     * @param notes Optional notes for the mammogram.
     * @return ResponseEntity with a Map representing the response and HTTP status 201 (Created).
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> uploadMammogram(
            @RequestParam("patientId") Long patientId,
            @RequestPart("imageFile") MultipartFile imageFile,
            @RequestParam(value = "notes", required = false) String notes) {
        log.info("Received request to upload mammogram for patient ID: {}", patientId);

        MammogramUploadRequest uploadRequest = new MammogramUploadRequest();
        uploadRequest.setPatientId(patientId);
        uploadRequest.setImageFile(imageFile);
        uploadRequest.setNotes(notes);

        MammogramResponse responseData = mammogramService.uploadMammogram(uploadRequest);
        log.info("Mammogram uploaded successfully with ID: {}", responseData.getId());

        // Construct the HashMap response
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Mammogram uploaded successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.CREATED.value());

        return new ResponseEntity<>(responseMap, HttpStatus.CREATED);
    }

    /**
     * Retrieves a mammogram by its ID.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     *
     * @param id The ID of the mammogram to retrieve.
     * @return ResponseEntity with a Map representing the response and HTTP status 200 (OK).
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> getMammogramById(@PathVariable Long id) {
        log.info("Received request to get mammogram by ID: {}", id);
        MammogramResponse responseData = mammogramService.getMammogramById(id);

        // Construct the HashMap response
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Mammogram retrieved successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Retrieves all mammograms in the system.
     * Accessible by ADMIN, RADIOLOGIST.
     *
     * @return ResponseEntity with a Map representing the response and HTTP status 200 (OK).
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> getAllMammograms() {
        log.info("Received request to get all mammograms.");
        List<MammogramResponse> responseData = mammogramService.getAllMammograms();

        // Construct the HashMap response
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "All mammograms retrieved successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Retrieves all mammograms for a specific patient.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     *
     * @param patientId The ID of the patient.
     * @return ResponseEntity with a Map representing the response and HTTP status 200 (OK).
     */
    @GetMapping("/patient/{patientId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> getMammogramsByPatientId(@PathVariable Long patientId) {
        log.info("Received request to get mammograms for patient ID: {}", patientId);
        List<MammogramResponse> responseData = mammogramService.getMammogramsByPatientId(patientId);

        // Construct the HashMap response
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Mammograms for patient " + patientId + " retrieved successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Updates the notes for a specific mammogram.
     * Accessible by ADMIN, DOCTOR, RADIOLOGIST.
     *
     * @param id The ID of the mammogram to update.
     * @param notes The new notes for the mammogram.
     * @return ResponseEntity with a Map representing the response and HTTP status 200 (OK).
     */
    @PutMapping("/{id}/notes")
    @PreAuthorize("hasAnyRole('ADMIN', 'DOCTOR', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> updateMammogramNotes(
            @PathVariable Long id,
            @RequestParam("notes") String notes) {
        log.info("Received request to update notes for mammogram ID: {}", id);
        MammogramResponse responseData = mammogramService.updateMammogram(id, notes);

        // Construct the HashMap response
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Mammogram notes updated successfully.");
        responseMap.put("data", responseData);
        responseMap.put("status", HttpStatus.OK.value());

        return ResponseEntity.ok(responseMap);
    }

    /**
     * Deletes a mammogram by its ID, including the associated image file.
     * Accessible by ADMIN, RADIOLOGIST.
     *
     * @param id The ID of the mammogram to delete.
     * @return ResponseEntity with a Map representing the response and HTTP status 204 (No Content).
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'RADIOLOGIST')")
    public ResponseEntity<Map<String, Object>> deleteMammogram(@PathVariable Long id) {
        log.info("Received request to delete mammogram by ID: {}", id);
        mammogramService.deleteMammogram(id);
        log.info("Mammogram ID {} deleted successfully.", id);

        // Construct the HashMap response
        Map<String, Object> responseMap = new HashMap<>();
        responseMap.put("success", true);
        responseMap.put("message", "Mammogram ID " + id + " deleted successfully.");
        // No 'data' field for 204 No Content typically, but can be added if desired
        responseMap.put("status", HttpStatus.NO_CONTENT.value());

        return new ResponseEntity<>(responseMap, HttpStatus.NO_CONTENT); // 204 No Content
    }
}