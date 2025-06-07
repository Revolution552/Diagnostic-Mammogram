package com.diagnostic.mammogram.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class PatientRequest {

    @NotBlank(message = "Name is mandatory")
    @Size(max = 100, message = "Name cannot exceed 100 characters")
    private String name;

    @Min(value = 18, message = "Patient must be at least 18 years old")
    @Max(value = 120, message = "Age cannot exceed 120 years")
    private int age;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
    private String gender;

    @Email(message = "Invalid email format")
    private String contactInfo;

    @Size(max = 500, message = "Medical history cannot exceed 500 characters")
    private String medicalHistory;
}