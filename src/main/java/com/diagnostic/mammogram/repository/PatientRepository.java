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

    /**
     * Finds a patient by their full name and contact information.
     * Used for precise lookups.
     * @param name The patient's full name.
     * @param contactInfo The patient's contact information (e.g., phone, email).
     * @return An Optional containing the found Patient, or empty if not found.
     */
    Optional<Patient> findByFullNameAndContactInfo(String name, String contactInfo);

    /**
     * Finds all patients whose age is greater than or equal to the specified age.
     * @param age The minimum age.
     * @return A list of patients matching the criteria.
     */
    List<Patient> findByAgeGreaterThanEqual(int age);

    // Gender-specific queries
    /**
     * Finds all patients of a specific gender.
     * @param gender The gender to search for (e.g., "Male", "Female").
     * @return A list of patients matching the gender.
     */
    List<Patient> findByGender(String gender);

    /**
     * Counts the number of patients for a specific gender.
     * @param gender The gender to count.
     * @return The count of patients with the specified gender.
     */
    long countByGender(String gender);

    // Medical history search
    /**
     * Finds patients whose medical history contains a specific keyword, ignoring case.
     * @param keyword The keyword to search within medical history.
     * @return A list of patients with matching medical history.
     */
    List<Patient> findByMedicalHistoryContainingIgnoreCase(String keyword);

    // Date range queries (assuming 'createdAt' is a field in Patient for creation timestamp)
    /**
     * Finds patients created within a specific date range.
     * @param startDate The start date of the range (inclusive).
     * @param endDate The end date of the range (inclusive).
     * @return A list of patients created within the specified date range.
     */
    List<Patient> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    /**
     * Finds patients created after a specific date.
     * @param date The date after which patients were created.
     * @return A list of patients created after the specified date.
     */
    List<Patient> findByCreatedAtAfter(LocalDateTime date);

    /**
     * Finds patients created before a specific date.
     * @param date The date before which patients were created.
     * @return A list of patients created before the specified date.
     */
    List<Patient> findByCreatedAtBefore(LocalDateTime date);

    // Mammogram result queries
    /**
     * Finds distinct patients who have at least one mammogram with a specific result.
     * This query explicitly joins with mammograms and filters by the result, then orders by patient full name.
     * @param result The mammogram result to filter by (e.g., "Benign", "Malignant").
     * @return A list of distinct patients with matching mammogram results.
     */
    @Query("SELECT DISTINCT p FROM Patient p JOIN p.mammograms m WHERE m.result = :result ORDER BY p.fullName")
    List<Patient> findByMammograms_Result(@Param("result") String result);

    /**
     * Finds patients who have at least one mammogram with a specific result using EXISTS subquery.
     * This is an alternative to JOIN for checking existence.
     * @param result The mammogram result to check for.
     * @return A list of patients with at least one mammogram matching the result.
     */
    @Query("SELECT p FROM Patient p WHERE EXISTS (SELECT 1 FROM p.mammograms m WHERE m.result = :result)")
    List<Patient> findPatientsWithAtLeastOneMammogramResult(@Param("result") String result);

    // Projection queries
    /**
     * Retrieves specific demographic information (full name, age, gender) for patients within a given age range.
     * The result is returned as a List of Object arrays.
     * @param minAge The minimum age for the range.
     * @param maxAge The maximum age for the range.
     * @return A list of Object arrays, each containing [fullName, age, gender].
     */
    @Query("SELECT p.fullName, p.age, p.gender FROM Patient p WHERE p.age BETWEEN :minAge AND :maxAge")
    List<Object[]> findPatientDemographicsByAgeRange(@Param("minAge") int minAge, @Param("maxAge") int maxAge);

    // Advanced search combining multiple criteria
    /**
     * Performs an advanced search for patients based on optional criteria: full name (partial match),
     * age range, and gender. If a parameter is NULL, it is ignored in the query.
     * @param fullName The full name to search for (partial, case-insensitive). Can be null.
     * @param minAge The minimum age (inclusive). Can be null.
     * @param maxAge The maximum age (inclusive). Can be null.
     * @param gender The gender to filter by. Can be null.
     * @return A list of patients matching the specified criteria.
     */
    @Query("SELECT p FROM Patient p WHERE " +
            "(:fullName IS NULL OR p.fullName LIKE %:fullName%) AND " + // Corrected parameter name from :name to :fullName
            "(:minAge IS NULL OR p.age >= :minAge) AND " +
            "(:maxAge IS NULL OR p.age <= :maxAge) AND " +
            "(:gender IS NULL OR p.gender = :gender)")
    List<Patient> advancedSearch(
            @Param("fullName") String fullName, // Renamed from 'name' to 'fullName' to match @Query param
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("gender") String gender);

    // Native queries for complex operations (using SQL directly)
    /**
     * Finds patients based on a mammogram result and the mammogram's upload date range using a native SQL query.
     * This query is database-specific SQL.
     * @param result The mammogram result.
     * @param startDate The start date for mammogram upload.
     * @param endDate The end date for mammogram upload.
     * @return A list of patients matching the criteria via native query.
     */
    @Query(value = """
           SELECT p.* FROM patients p
           JOIN mammograms m ON p.id = m.patient_id
           WHERE m.result = :result
           AND m.upload_date BETWEEN :startDate AND :endDate
           ORDER BY p.full_name -- Assuming 'full_name' is the column name in the 'patients' table
           """, nativeQuery = true)
    List<Patient> findByMammogramResultAndDateRange(
            @Param("result") String result,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // Existence checks
    /**
     * Checks if a patient exists with the given contact information.
     * @param contactInfo The contact information to check.
     * @return True if a patient exists with this contact info, false otherwise.
     */
    boolean existsByContactInfo(String contactInfo);

    /**
     * Checks if a patient exists with the given full name and medical history containing a specific term.
     * @param fullName The full name of the patient.
     * @param medicalHistoryTerm The term to search within medical history.
     * @return True if a patient exists matching both criteria, false otherwise.
     */
    boolean existsByFullNameAndMedicalHistoryContaining(String fullName, String medicalHistoryTerm);

    // Custom delete operations
    /**
     * Deletes patients by their contact information.
     * This operation will be executed within a transaction.
     * @param contactInfo The contact information of the patient(s) to delete.
     */
    void deleteByContactInfo(String contactInfo);

    // Statistical queries
    /**
     * Calculates the average age of all patients.
     * @return The average age as a Double.
     */
    @Query("SELECT AVG(p.age) FROM Patient p")
    Double findAverageAge();

    /**
     * Counts the number of patients grouped by gender.
     * @return A list of Object arrays, each containing [gender, count].
     */
    @Query("SELECT p.gender, COUNT(p) FROM Patient p GROUP BY p.gender")
    List<Object[]> countPatientsByGender();
}