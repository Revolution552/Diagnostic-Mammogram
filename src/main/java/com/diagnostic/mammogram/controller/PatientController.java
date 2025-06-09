package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.PatientRequest;
import com.diagnostic.mammogram.dto.response.PatientResponse;
import com.diagnostic.mammogram.exception.PatientNotFoundException;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.service.PatientService;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    @GetMapping("/get")
    public ResponseEntity<Map<String, Object>> getAllPatients(
            @PageableDefault(size = 10) Pageable pageable) {
        Map<String, Object> response = new HashMap<>();
        log.info("Fetching all patients with pagination - page: {}, size: {}",
                pageable.getPageNumber(), pageable.getPageSize());

        try {
            Page<PatientResponse> patients = patientService.getAllPatients(pageable);
            log.debug("Successfully fetched {} patients", patients.getTotalElements());

            response.put("status", "success");
            response.put("message", "Patients fetched successfully");
            response.put("data", patients);
            response.put("pageInfo", Map.of(
                    "currentPage", patients.getNumber(),
                    "totalPages", patients.getTotalPages(),
                    "totalItems", patients.getTotalElements()
            ));

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            log.error("Error fetching patients: {}", ex.getMessage(), ex);
            response.put("status", "error");
            response.put("message", "Error fetching patients: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> createPatient(
            @Valid @RequestBody PatientRequest request) {

        final String operation = "CREATE_PATIENT";
        Map<String, Object> response = new HashMap<>();
        log.info("[{}] Creating new patient with data: {}", operation, request);

        try {
            long startTime = System.currentTimeMillis();
            PatientResponse createdPatient = patientService.createPatient(request);
            long duration = System.currentTimeMillis() - startTime;

            log.info("[{}] Patient created successfully with ID: {} in {} ms",
                    operation, createdPatient.getId(), duration);

            response.put("status", "success");
            response.put("message", "Patient created successfully");
            response.put("patientId", createdPatient.getId());
            response.put("data", createdPatient);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("processingTimeMs", duration);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (DataIntegrityViolationException ex) {
            String errorMessage = "Contact information already exists";
            if (ex.getMessage().contains("patients.UK5eoemimnyup3a88rtkpkq7rpf")) {
                errorMessage = "Phone Number already registered: " + request.getContactInfo();
            }

            log.warn("[{}] Data integrity violation: {}", operation, errorMessage);

            response.put("status", "error");
            response.put("errorType", "DUPLICATE_ENTRY");
            response.put("message", errorMessage);
            response.put("conflictField", "contactInfo");
            response.put("conflictValue", request.getContactInfo());
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (ConstraintViolationException ex) {
            log.warn("[{}] Constraint violation: {}", operation, ex.getMessage());

            response.put("status", "error");
            response.put("errorType", "VALIDATION_ERROR");
            response.put("message", "Validation failed: " + ex.getMessage());
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);

        } catch (Exception ex) {
            log.error("[{}] Unexpected error: {}", operation, ex.getMessage(), ex);

            response.put("status", "error");
            response.put("errorType", "INTERNAL_SERVER_ERROR");
            response.put("message", "An unexpected error occurred while creating patient");
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<Map<String, Object>> getPatientById(@PathVariable Long id) {
        final String operation = "GET_PATIENT";
        Map<String, Object> response = new HashMap<>();
        log.info("[{}] Fetching patient with ID: {}", operation, id);

        try {
            long startTime = System.currentTimeMillis();
            PatientResponse patient = patientService.getPatientById(id);
            long duration = System.currentTimeMillis() - startTime;

            log.debug("[{}] Successfully fetched patient with ID: {} in {} ms",
                    operation, id, duration);

            response.put("status", "success");
            response.put("message", "Patient details retrieved successfully");
            response.put("patientId", id);
            response.put("data", patient);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("processingTimeMs", duration);

            return ResponseEntity.ok(response);

        } catch (PatientNotFoundException | ResourceNotFoundException ex) {
            // Handle both not-found cases the same way
            log.warn("[{}] Patient not found with ID: {}", operation, id);

            response.put("status", "error");
            response.put("errorType", "PATIENT_NOT_FOUND");
            response.put("message", "No patient exists with ID: " + id);
            response.put("requestedId", id);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (Exception ex) {
            log.error("[{}] Unexpected error fetching patient ID: {} - {}",
                    operation, id, ex.getMessage(), ex);

            response.put("status", "error");
            response.put("errorType", "INTERNAL_SERVER_ERROR");
            response.put("message", "An unexpected error occurred while fetching patient");
            response.put("patientId", id);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {

        final String operation = "UPDATE_PATIENT";
        Map<String, Object> response = new HashMap<>();
        log.info("[{}] Attempting to update patient with ID: {}, data: {}", operation, id, request);

        try {
            long startTime = System.currentTimeMillis();
            PatientResponse updatedPatient = patientService.updatePatient(id, request);
            long duration = System.currentTimeMillis() - startTime;

            log.info("[{}] Successfully updated patient with ID: {} in {} ms",
                    operation, id, duration);

            response.put("status", "success");
            response.put("message", "Patient updated successfully");
            response.put("patientId", id);
            response.put("data", updatedPatient);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("processingTimeMs", duration);

            return ResponseEntity.ok(response);

        } catch (PatientNotFoundException | ResourceNotFoundException ex) {
            // Handle both not-found exceptions the same way
            log.warn("[{}] Failed to update - Patient not found with ID: {}", operation, id);

            response.put("status", "error");
            response.put("errorType", "PATIENT_NOT_FOUND");
            response.put("message", "No patient exists with ID: " + id);
            response.put("requestedId", id);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (DataIntegrityViolationException ex) {
            log.error("[{}] Data integrity violation while updating patient ID: {} - {}",
                    operation, id, ex.getMessage());

            response.put("status", "error");
            response.put("errorType", "DATA_INTEGRITY_VIOLATION");
            response.put("message", "Data conflict - possible duplicate or constraint violation");
            response.put("patientId", id);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception ex) {
            log.error("[{}] Unexpected error updating patient ID: {} - {}",
                    operation, id, ex.getMessage(), ex);

            response.put("status", "error");
            response.put("errorType", "INTERNAL_SERVER_ERROR");
            response.put("message", "An unexpected error occurred while updating patient");
            response.put("patientId", id);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deletePatient(@PathVariable Long id) {
        final String operation = "DELETE_PATIENT";
        Map<String, Object> response = new HashMap<>();
        log.info("[{}] Attempting to delete patient with ID: {}", operation, id);

        try {
            long startTime = System.currentTimeMillis();
            patientService.deletePatient(id);
            long duration = System.currentTimeMillis() - startTime;

            log.info("[{}] Successfully deleted patient with ID: {} in {} ms",
                    operation, id, duration);

            response.put("status", "success");
            response.put("message", "Patient deleted successfully");
            response.put("patientId", id);
            response.put("timestamp", LocalDateTime.now().toString());
            response.put("processingTimeMs", duration);

            return ResponseEntity.ok(response);

        } catch (PatientNotFoundException | ResourceNotFoundException ex) {
            // Handle both not-found exceptions consistently
            log.warn("[{}] Patient not found with ID: {}", operation, id);

            response.put("status", "error");
            response.put("errorType", "PATIENT_NOT_FOUND");
            response.put("message", "No patient exists with ID: " + id);
            response.put("requestedId", id);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);

        } catch (DataIntegrityViolationException ex) {
            log.error("[{}] Data integrity violation while deleting patient ID: {} - {}",
                    operation, id, ex.getMessage());

            response.put("status", "error");
            response.put("errorType", "DATA_INTEGRITY_VIOLATION");
            response.put("message", "Cannot delete patient due to existing references");
            response.put("patientId", id);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.CONFLICT).body(response);

        } catch (Exception ex) {
            log.error("[{}] Unexpected error while deleting patient with ID: {} - {}",
                    operation, id, ex.getMessage(), ex);

            response.put("status", "error");
            response.put("errorType", "INTERNAL_SERVER_ERROR");
            response.put("message", "An unexpected error occurred while deleting patient");
            response.put("patientId", id);
            response.put("timestamp", LocalDateTime.now().toString());

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}