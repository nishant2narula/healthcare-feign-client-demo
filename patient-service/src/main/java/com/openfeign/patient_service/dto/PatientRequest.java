package com.openfeign.patient_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * ================================================================
 * DTO — PatientRequest
 * ================================================================
 * Request body for POST /api/patients and PUT /api/patients/{id}.
 * <p>
 * Notice: 'id' is NOT in this DTO.
 * The client never sends an ID when creating a patient — the service
 * assigns one. For updates, the ID comes from the URL path variable,
 * not the body. This prevents clients from accidentally changing IDs.
 * ================================================================
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PatientRequest {

    @NotBlank(message = "Patient name is required")
    private String name;

    @Email
    @NotNull
    private String email;

    private String phone;

    @NotNull(message = "Date of birth is required")
    private LocalDate dateOfBirth;

    private String bloodGroup;
    private String address;
    private List<String> medicalHistory;
    private String insuranceProvider;
    private String insurancePolicyNumber;

}
