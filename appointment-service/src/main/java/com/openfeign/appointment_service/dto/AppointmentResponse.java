package com.openfeign.appointment_service.dto;

import com.openfeign.appointment_service.model.AppointmentModels;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ================================================================
 * DTO — AppointmentResponse
 * ================================================================
 * The RICH response returned from all appointment endpoints.
 * Combines data from THREE sources into one response object:
 *
 *   1. Local appointment data (id, status, dateTime, reason, notes)
 *      → from appointment-service's own in-memory store
 *
 *   2. Doctor details (name, specialization, availability...)
 *      → fetched from doctor-service via DoctorClient (Feign)
 *
 *   3. Patient details (name, email, medical history...)
 *      → fetched from patient-service via PatientClient (Feign)
 *
 * This "aggregate on read" pattern is a core Feign use case:
 * instead of making the client call three separate services,
 * the appointment-service does the aggregation server-side.
 *
 * Example JSON response:
 * {
 *   "id": 1,
 *   "status": "SCHEDULED",
 *   "appointmentDateTime": "2025-12-15T10:00:00",
 *   "reason": "Annual checkup",
 *   "doctor": {
 *     "id": 1,
 *     "name": "Dr. Sarah Mitchell",   ← from doctor-service via Feign
 *     "specialization": "Cardiologist"
 *   },
 *   "patient": {
 *     "id": 1,
 *     "name": "Alice Johnson",        ← from patient-service via Feign
 *     "bloodGroup": "A+"
 *   }
 * }
 * ================================================================
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponse {

    // Core appointment data — from local store
    private Long id;
    private AppointmentModels.AppointmentStatus status;
    private LocalDateTime appointmentDateTime;
    private String reason;
    private String notes;

    // Nested object populated by DoctorClient Feign call.
    // If doctor-service is down, this contains the fallback stub from
    // DoctorClientFallback.getDoctorById().
    private DoctorResponse doctor;

    // Nested object populated by PatientClient Feign call.
    // If patient-service is down, this contains the fallback stub from
    // PatientClientFallback.getPatientById().
    private PatientResponse patient;
}