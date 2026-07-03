package com.openfeign.appointment_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * ================================================================
 * DOMAIN MODEL — Appointment (inner class pattern)
 * ================================================================
 * Groups the Appointment entity and its status enum in one file.
 * This is a convenient pattern for small models that belong together.
 *
 * The Appointment entity stores ONLY the IDs of the doctor and patient,
 * not their full details. This is the correct microservices pattern:
 *   - Each service owns its own data
 *   - doctor-service owns doctor details (name, specialization, etc.)
 *   - patient-service owns patient details (name, history, etc.)
 *   - appointment-service stores only the references (doctorId, patientId)
 *
 * When a full AppointmentResponse is needed, we enrich by calling
 * DoctorClient and PatientClient via Feign — that's the whole point
 * of this demo. See AppointmentService.getAppointmentById() for how
 * this enrichment works.
 * ================================================================
 */

public class AppointmentModels {

    /**
     * Lifecycle states of an appointment.
     * Stored as a String in the in-memory map (Jackson serializes enums
     * to their name() by default: "SCHEDULED", "CONFIRMED" etc.)
     */
    public enum AppointmentStatus {
        SCHEDULED,   // Booked but not yet confirmed by the doctor
        CONFIRMED,   // Doctor has confirmed the time slot
        CANCELLED,   // Cancelled by patient or doctor
        COMPLETED    // The appointment has taken place
    }

    /**
     * The core Appointment entity.
     * Stored locally in appointment-service's ConcurrentHashMap.
     *
     * Note what's stored here:
     *   doctorId  → just the ID reference, not the Doctor object
     *   patientId → just the ID reference, not the Patient object
     *
     * Full Doctor and Patient details are fetched on demand via Feign
     * when building the AppointmentResponse. This avoids stale data
     * (if a doctor's name changes, the next Feign call gets the new name).
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Appointment {

        // Auto-assigned by AppointmentService using AtomicLong
        private Long id;

        // Foreign key reference to doctor-service — NOT the Doctor object
        private Long doctorId;

        // Foreign key reference to patient-service — NOT the Patient object
        private Long patientId;

        // Date and time of the appointment — LocalDateTime is timezone-unaware.
        // In production, use ZonedDateTime or store in UTC.
        private LocalDateTime appointmentDateTime;

        // Current lifecycle state
        private AppointmentStatus status;

        // Why the patient is coming in, e.g. "Annual cardiac checkup"
        private String reason;

        // Additional clinical notes
        private String notes;
    }
}
