package com.openfeign.appointment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.util.List;

/**
 * ================================================================
 * DTO — PatientResponse (in appointment-service)
 * ================================================================
 * Represents the patient data received from patient-service via
 * Feign's PatientClient.
 *
 * Deserialized from JSON by Jackson when PatientClient returns.
 * Included inside AppointmentResponse to give callers a full
 * picture of the appointment without needing a second API call.
 * ================================================================
 */


@Data
@Builder
@NoArgsConstructor  // required by Jackson
@AllArgsConstructor
public class PatientResponse {

    private Long id;
    private String name;
    private String email;
    private String phone;
    private LocalDate dateOfBirth;
    private String bloodGroup;

    // Medical history is included so appointment details show relevant context.
    // In a real app you'd carefully consider privacy implications of exposing this.
    private List<String> medicalHistory;

    private String insuranceProvider;
}