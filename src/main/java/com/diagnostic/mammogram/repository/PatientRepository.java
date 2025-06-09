package com.diagnostic.mammogram.repository;

import com.diagnostic.mammogram.model.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {

    // Basic CRUD operations are inherited from JpaRepository

    // Basic query methods
    Optional<Patient> findByFullNameAndContactInfo(String name, String contactInfo);

    List<Patient> findByAgeGreaterThanEqual(int age);

    // Gender-specific queries
    List<Patient> findByGender(String gender);
    long countByGender(String gender);

    // Medical history search
    List<Patient> findByMedicalHistoryContainingIgnoreCase(String keyword);

    // Date range queries
    List<Patient> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);
    List<Patient> findByCreatedAtAfter(LocalDateTime date);
    List<Patient> findByCreatedAtBefore(LocalDateTime date);

    // Mammogram result queries
    @Query("SELECT DISTINCT p FROM Patient p JOIN p.mammograms m WHERE m.result = :result ORDER BY p.fullName")
    List<Patient> findByMammogramResult(@Param("result") String result);

    @Query("SELECT p FROM Patient p WHERE EXISTS (SELECT 1 FROM p.mammograms m WHERE m.result = :result)")
    List<Patient> findPatientsWithAtLeastOneMammogramResult(@Param("result") String result);

    // Projection queries
    @Query("SELECT p.fullName, p.age, p.gender FROM Patient p WHERE p.age BETWEEN :minAge AND :maxAge")
    List<Object[]> findPatientDemographicsByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);

    // Advanced search combining multiple criteria
    @Query("SELECT p FROM Patient p WHERE " +
            "(:name IS NULL OR p.fullName LIKE %:name%) AND " +
            "(:minAge IS NULL OR p.age >= :minAge) AND " +
            "(:maxAge IS NULL OR p.age <= :maxAge) AND " +
            "(:gender IS NULL OR p.gender = :gender)")
    List<Patient> advancedSearch(
            @Param("fullName") String fullName,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("gender") String gender);

    // Native queries for complex operations
    @Query(value = """
           SELECT p.* FROM patients p 
           JOIN mammograms m ON p.id = m.patient_id
           WHERE m.result = :result 
           AND m.upload_date BETWEEN :startDate AND :endDate
           ORDER BY p.name
           """, nativeQuery = true)
    List<Patient> findByMammogramResultAndDateRange(
            @Param("result") String result,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Existence checks
    boolean existsByContactInfo(String contactInfo);
    boolean existsByFullNameAndMedicalHistoryContaining(String name, String medicalHistoryTerm);

    // Custom delete operations
    void deleteByContactInfo(String contactInfo);

    // Statistical queries
    @Query("SELECT AVG(p.age) FROM Patient p")
    Double findAverageAge();

    @Query("SELECT p.gender, COUNT(p) FROM Patient p GROUP BY p.gender")
    List<Object[]> countPatientsByGender();
}