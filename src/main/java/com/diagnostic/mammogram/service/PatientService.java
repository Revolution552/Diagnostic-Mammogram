package com.diagnostic.mammogram.service;

import com.diagnostic.mammogram.dto.request.PatientRequest;
import com.diagnostic.mammogram.dto.response.PatientResponse;
import com.diagnostic.mammogram.exception.ResourceNotFoundException;
import com.diagnostic.mammogram.model.Patient;
import com.diagnostic.mammogram.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PatientService {

    private final PatientRepository patientRepository;
    private final ModelMapper modelMapper;

    @Transactional
    public PatientResponse createPatient(PatientRequest request) {
        Patient patient = modelMapper.map(request, Patient.class);
        Patient savedPatient = patientRepository.save(patient);
        return modelMapper.map(savedPatient, PatientResponse.class);
    }

    @Transactional(readOnly = true)
    public Page<PatientResponse> getAllPatients(Pageable pageable) {
        return patientRepository.findAll(pageable)
                .map(patient -> modelMapper.map(patient, PatientResponse.class));
    }

    @Transactional(readOnly = true)
    public PatientResponse getPatientById(Long id) {
        return patientRepository.findById(id)
                .map(patient -> modelMapper.map(patient, PatientResponse.class))
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    @Transactional
    public PatientResponse updatePatient(Long id, PatientRequest request) {
        return patientRepository.findById(id)
                .map(patient -> {
                    modelMapper.map(request, patient);
                    return modelMapper.map(patientRepository.save(patient), PatientResponse.class);
                })
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
    }

    @Transactional
    public void deletePatient(Long id) {
        Patient patient = patientRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Patient not found with id: " + id));
        patientRepository.delete(patient);
    }
}