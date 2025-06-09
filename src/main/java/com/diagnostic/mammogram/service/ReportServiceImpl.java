package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.Mammogram;
import com.diagnostic.mammogram.model.Report;
import com.diagnostic.mammogram.model.User;
import com.diagnostic.mammogram.repository.MammogramRepository;
import com.diagnostic.mammogram.repository.ReportRepository;
import com.diagnostic.mammogram.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;
    private final MammogramRepository mammogramRepository;
    private final UserRepository userRepository;

    public ReportServiceImpl(ReportRepository reportRepository,
                             MammogramRepository mammogramRepository,
                             UserRepository userRepository) {
        this.reportRepository = reportRepository;
        this.mammogramRepository = mammogramRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public Report generateReport(Long mammogramId, String findings,
                                 String recommendations, String createdByUsername) {
        Mammogram mammogram = mammogramRepository.findById(mammogramId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Mammogram not found with id: " + mammogramId));

        User creator = userRepository.findByUsername(createdByUsername)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "User not found with username: " + createdByUsername));

        Report report = new Report();
        report.setMammogram(mammogram);
        report.setFindings(findings);
        report.setRecommendations(recommendations);
        report.setCreatedBy(creator);
        report.setCreatedAt(LocalDateTime.now());

        return reportRepository.save(report);
    }

    public Report getReportByMammogramId(Long mammogramId) {
        return reportRepository.findByMammogramId(mammogramId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Report not found for mammogram ID: " + mammogramId));
    }
}