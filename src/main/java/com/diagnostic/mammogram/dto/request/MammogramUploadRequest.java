package com.diagnostic.mammogram.dto.request;

import lombok.Data;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

// Import your custom exceptions if you use them here for validation annotations
// import com.mammogram.exception.CustomValidationException;

@Data // Lombok: Generates getters, setters, toString, equals, hashCode
public class MammogramUploadRequest {

    @NotNull(message = "Patient ID cannot be null")
    private Long patientId;

    @NotNull(message = "Mammogram image file cannot be null")
    private MultipartFile imageFile; // Spring's representation of an uploaded file

    private String notes; // Optional notes for the mammogram
}