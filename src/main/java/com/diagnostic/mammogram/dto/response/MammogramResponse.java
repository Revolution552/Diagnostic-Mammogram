package com.diagnostic.mammogram.dto.response;

import lombok.Data;

import java.util.Date;


@Data
public class MammogramResponse {
    private Long id;
    private String imagePath;
    private Date uploadDate;
    private String notes;
    // Add patient details you want to expose
    private Long patientId;
    private String patientName;

    // constructor, getters, setters
}