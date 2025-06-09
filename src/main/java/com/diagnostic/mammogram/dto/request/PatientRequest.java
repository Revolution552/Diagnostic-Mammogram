package com.diagnostic.mammogram.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class PatientRequest {

    @NotBlank(message = "Full name is mandatory")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    @Pattern(regexp = "^[a-zA-Z\\s\\-'.]+$", message = "Full name can only contain letters, spaces, hyphens, apostrophes, and periods")
    private String fullName;

    @NotBlank(message = "Gender is required")
    @Pattern(regexp = "^(Male|Female|Other)$", message = "Gender must be Male, Female, or Other")
    private String gender;

    @NotNull(message = "Date of birth is required")
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;

    @Size(max = 100, message = "Place of birth cannot exceed 100 characters")
    private String placeOfBirth;

    @Min(value = 18, message = "Patient must be at least 18 years old")
    @Max(value = 120, message = "Age cannot exceed 120 years")
    private int age;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    @Pattern(
            regexp = "^(\\+255|0)[ -]?[1-9]\\d{2}[ -]?\\d{3}[ -]?\\d{3}$",
            message = "Invalid Tanzanian phone number format. Valid formats: +255712345678, 0712345678, 0712 345 678, or +255 712 345 678"
    )
    private String contactInfo;

    @Size(max = 50, message = "Education cannot exceed 50 characters")
    private String education;

    @Size(max = 500, message = "Medical history cannot exceed 500 characters")
    private String medicalHistory;

    @AssertTrue(message = "Age must match date of birth")
    private boolean isAgeValid() {
        if (dateOfBirth == null) {
            return false;
        }
        LocalDate today = LocalDate.now();
        int calculatedAge = today.getYear() - dateOfBirth.getYear();
        if (dateOfBirth.plusYears(calculatedAge).isAfter(today)) {
            calculatedAge--;
        }
        return calculatedAge == age;
    }
}