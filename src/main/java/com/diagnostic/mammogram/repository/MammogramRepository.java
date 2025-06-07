package com.diagnostic.mammogram.repository;

import com.diagnostic.mammogram.model.Mammogram;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MammogramRepository extends JpaRepository<Mammogram, Long> {
    List<Mammogram> findByPatientId(Long patientId);
}