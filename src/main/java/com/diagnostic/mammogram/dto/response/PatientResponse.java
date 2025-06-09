package com.diagnostic.mammogram.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class PatientResponse {
    private Long id;
    private String fullName;
    private String gender;
    private LocalDate dateOfBirth;
    private String placeOfBirth;
    private int age;
    private String contactInfo;
    private String education;
    private String medicalHistory;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
}