package com.diagnostic.mammogram.repository;

import com.diagnostic.mammogram.model.AIDiagnosisResult;
import com.diagnostic.mammogram.model.Mammogram;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List; // Import List
import java.util.Optional;

@Repository
public interface AIDiagnosisResultRepository extends JpaRepository<AIDiagnosisResult, Long> {

    // Find an AI diagnosis result by its associated mammogram
    Optional<AIDiagnosisResult> findByMammogram(Mammogram mammogram);

    // NEW: Find all AI diagnosis results for a specific patient ID
    List<AIDiagnosisResult> findByPatientId(Long patientId);
}
