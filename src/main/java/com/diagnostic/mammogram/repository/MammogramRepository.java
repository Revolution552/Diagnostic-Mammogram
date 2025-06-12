package com.diagnostic.mammogram.repository;

import com.diagnostic.mammogram.model.Mammogram;
import com.diagnostic.mammogram.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository // Marks this interface as a Spring Data JPA repository
public interface MammogramRepository extends JpaRepository<Mammogram, Long> {

    // You get basic CRUD operations for free (save, findById, findAll, deleteById, etc.)
    // from extending JpaRepository<Mammogram, Long>

    // --- Custom Query Methods (Examples) ---

    // Find all mammograms associated with a specific patient
    List<Mammogram> findByPatient(Patient patient);

    // Find all mammograms associated with a specific patient, ordered by upload date descending
    List<Mammogram> findByPatientOrderByDateUploadedDesc(Patient patient);

    // Find a mammogram by its image path (since imagePath is unique)
    Optional<Mammogram> findByImagePath(String imagePath);

    // Find mammograms uploaded within a specific date range
    List<Mammogram> findByDateUploadedBetween(LocalDateTime startDate, LocalDateTime endDate);

    // Count mammograms for a specific patient
    long countByPatient(Patient patient);

    // You can add more custom query methods here as needed, following Spring Data JPA conventions
    // For example:
    // List<Mammogram> findByNotesContainingIgnoreCase(String keyword);
}