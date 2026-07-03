package com.openfeign.patient_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * ================================================================
 * DOMAIN MODEL — Patient
 * ================================================================
 * Represents a Patient in the healthcare system.
 * <p>
 * LocalDate (not LocalDateTime) for dateOfBirth:
 * A birth date has no time component. Using LocalDate instead of
 * LocalDateTime avoids timezone confusion and is more semantically correct.
 * Jackson serializes LocalDate as "1985-03-15" (ISO-8601 format).
 * <p>
 * medicalHistory as List<String>:
 * Stores past diagnoses like ["Hypertension", "Diabetes Type 2"].
 * In a real system this would be a separate table/collection with
 * richer data (date, treating doctor, treatment plan etc.).
 * Using a simple String list keeps the demo focused on Feign.
 * ================================================================
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient {
    private Long id;
    private String name;
    private String email;
    private String phone;

    // Birth date — used to calculate age and for medical records
    private LocalDate dateOfBirth;

    private String bloodGroup;

    private String address;

    // List of past diagnoses, e.g. ["Hypertension", "Seasonal Allergies"]
    private List<String> medicalHistory;

    // Insurance details — used for billing
    private String insuranceProvider;
    private String insurancePolicyNumber;
}
