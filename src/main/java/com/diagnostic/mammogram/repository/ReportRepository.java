package com.diagnostic.mammogram.repository;

import com.diagnostic.mammogram.model.Mammogram;
import com.diagnostic.mammogram.model.Report;
import com.diagnostic.mammogram.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository // Marks this interface as a Spring Data JPA repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    // Find a report by its associated mammogram. Since it's one-to-one and unique, Optional is good.
    Optional<Report> findByMammogram(Mammogram mammogram);

    // Find all reports created by a specific user (radiologist/doctor)
    List<Report> findByCreatedBy(User createdBy);

    // Find all reports for a specific patient (through mammogram's patient)
    // This requires a JOIN in JPQL or a derived query
    List<Report> findByMammogramPatientId(Long patientId);

    // You might also add methods like:
    // List<Report> findByStatus(ReportStatus status);
    // List<Report> findByReportDateBetween(LocalDateTime startDate, LocalDateTime endDate);
}