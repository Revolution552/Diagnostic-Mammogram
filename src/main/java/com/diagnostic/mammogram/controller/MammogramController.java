package com.diagnostic.mammogram.controller;

import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.Mammogram;
import com.diagnostic.mammogram.model.Patient;
import com.diagnostic.mammogram.repository.MammogramRepository;
import com.diagnostic.mammogram.repository.PatientRepository;
import com.diagnostic.mammogram.service.ImageStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;

@RestController
@RequestMapping("/api/mammograms")
@RequiredArgsConstructor
public class MammogramController {

    private final MammogramRepository mammogramRepository;
    private final PatientRepository patientRepository;
    private final ImageStorageService imageStorageService;

    @PostMapping("/upload/{patientId}")
    public ResponseEntity<Mammogram> uploadMammogram(
            @PathVariable Long patientId,
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String notes) {

        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found"));

        String imagePath = imageStorageService.storeImage(file);

        Mammogram mammogram = new Mammogram();
        mammogram.setPatient(patient);
        mammogram.setImagePath(imagePath);
        mammogram.setUploadDate(new Date());
        mammogram.setNotes(notes);

        return ResponseEntity.ok(mammogramRepository.save(mammogram));
    }
}