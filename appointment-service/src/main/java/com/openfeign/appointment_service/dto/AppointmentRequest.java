package com.openfeign.appointment_service.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * ================================================================
 * DTO — AppointmentRequest
 * ================================================================
 * Request body for POST /api/appointments (book a new appointment).
 *
 * The client sends this JSON:
 * {
 *   "doctorId": 1,
 *   "patientId": 2,
 *   "appointmentDateTime": "2025-12-15T10:00:00",
 *   "reason": "Annual checkup",
 *   "notes": "Patient prefers morning slots"
 * }
 *
 * Notice: no 'status' field — the service always sets SCHEDULED on creation.
 * Notice: no 'id' field — the service assigns it automatically.
 *
 * VALIDATION:
 *   @NotNull on doctorId and patientId → returns 400 if missing
 *   @Future on appointmentDateTime → returns 400 if date is in the past
 *   These are enforced by @Valid in AppointmentController.createAppointment()
 *
 * FEIGN CHAIN TRIGGERED BY THIS REQUEST:
 *   POST /api/appointments with this body
 *       ↓
 *   AppointmentService.createAppointment(request)
 *       ↓ Feign Call #1
 *   doctorClient.getDoctorById(request.getDoctorId())
 *       → GET http://localhost:8082/api/doctors/{id}
 *       ↓ Feign Call #2
 *   patientClient.getPatientById(request.getPatientId())
 *       → GET http://localhost:8083/api/patients/{id}
 *       ↓
 *   Appointment saved + AppointmentResponse returned
 * ================================================================
 */


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentRequest {

    // ID of the doctor to book with — must exist in doctor-service
    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    // ID of the patient making the appointment — must exist in patient-service
    @NotNull(message = "Patient ID is required")
    private Long patientId;

    // Must be a future date/time — @Future handles this validation
    @NotNull
    @Future(message = "Appointment must be scheduled in the future")
    private LocalDateTime appointmentDateTime;

    // Why the patient is booking — optional but recommended
    private String reason;

    // Additional notes from the patient — optional
    private String notes;
}