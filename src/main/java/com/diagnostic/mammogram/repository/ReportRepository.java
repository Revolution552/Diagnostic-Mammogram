package com.diagnostic.mammogram.repository;

import com.diagnostic.mammogram.model.Report;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@SuppressWarnings("ALL")
public interface ReportRepository extends JpaRepository<Report, Long> {

    // Option 1: Entity Graph approach (recommended)
    @EntityGraph(attributePaths = {"mammogram", "mammogram.patient"})
    Optional<Report> findWithDetailsByMammogramId(Long mammogramId);

    // Option 2: JPQL query approach (alternative)
    @Query("SELECT r FROM Report r JOIN FETCH r.mammogram m JOIN FETCH m.patient WHERE m.id = :mammogramId")
    Optional<Report> findByMammogramIdFetchAll(@Param("mammogramId") Long mammogramId);

    // Option 3: Simple find by mammogram ID (basic)
    Optional<Report> findByMammogramId(Long mammogramId);

    // Additional useful queries
    boolean existsByMammogramId(Long mammogramId);

    @Query("SELECT COUNT(r) > 0 FROM Report r WHERE r.mammogram.id = :mammogramId AND r.finalized = true")
    boolean existsFinalizedReportByMammogramId(@Param("mammogramId") Long mammogramId);
}