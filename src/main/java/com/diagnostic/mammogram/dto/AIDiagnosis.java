package com.diagnostic.mammogram.dto;

import lombok.Data;

import java.util.List;

@Data
public class AIDiagnosis {
    private String findings;
    private double confidenceScore;
    private String riskCategory;
    private List<String> suspiciousAreas;
}