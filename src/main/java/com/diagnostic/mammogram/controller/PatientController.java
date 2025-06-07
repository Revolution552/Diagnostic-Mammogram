package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.dto.request.PatientRequest;
import com.diagnostic.mammogram.dto.response.PatientResponse;
import com.diagnostic.mammogram.exception.PatientNotFoundException;
import com.diagnostic.mammogram.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
        Map<String, Object> response = new HashMap<>();
        log.info("Creating new patient with data: {}", request);

        try {
            PatientResponse createdPatient = patientService.createPatient(request);
            log.info("Patient created successfully with ID: {}", createdPatient.getId());

            response.put("status", "success");
            response.put("message", "Patient created successfully");
            response.put("data", createdPatient);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception ex) {
            log.error("Error creating patient: {}", ex.getMessage(), ex);
            response.put("status", "error");
            response.put("message", "Error creating patient: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/get/{id}")
    public ResponseEntity<Map<String, Object>> getPatientById(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        log.info("Fetching patient with ID: {}", id);

        try {
            PatientResponse patient = patientService.getPatientById(id);
            log.debug("Successfully fetched patient with ID: {}", id);

            response.put("status", "success");
            response.put("message", "Patient fetched successfully");
            response.put("data", patient);

            return ResponseEntity.ok(response);
        } catch (PatientNotFoundException ex) {
            log.warn("Patient not found with ID: {}", id);
            response.put("status", "error");
            response.put("message", "Patient not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception ex) {
            log.error("Error fetching patient with ID: {} - {}", id, ex.getMessage(), ex);
            response.put("status", "error");
            response.put("message", "Error fetching patient: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Map<String, Object>> updatePatient(
            @PathVariable Long id,
            @Valid @RequestBody PatientRequest request) {
        Map<String, Object> response = new HashMap<>();
        log.info("Updating patient with ID: {}, data: {}", id, request);

        try {
            PatientResponse updatedPatient = patientService.updatePatient(id, request);
            log.info("Patient with ID: {} updated successfully", id);

            response.put("status", "success");
            response.put("message", "Patient updated successfully");
            response.put("data", updatedPatient);

            return ResponseEntity.ok(response);
        } catch (PatientNotFoundException ex) {
            log.warn("Patient not found for update with ID: {}", id);
            response.put("status", "error");
            response.put("message", "Patient not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception ex) {
            log.error("Error updating patient with ID: {} - {}", id, ex.getMessage(), ex);
            response.put("status", "error");
            response.put("message", "Error updating patient: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<Map<String, Object>> deletePatient(@PathVariable Long id) {
        Map<String, Object> response = new HashMap<>();
        log.info("Deleting patient with ID: {}", id);

        try {
            patientService.deletePatient(id);
            log.info("Patient with ID: {} deleted successfully", id);

            response.put("status", "success");
            response.put("message", "Patient deleted successfully");

            return ResponseEntity.ok(response);
        } catch (PatientNotFoundException ex) {
            log.warn("Patient not found for deletion with ID: {}", id);
            response.put("status", "error");
            response.put("message", "Patient not found with ID: " + id);
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
        } catch (Exception ex) {
            log.error("Error deleting patient with ID: {} - {}", id, ex.getMessage(), ex);
            response.put("status", "error");
            response.put("message", "Error deleting patient: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }
}