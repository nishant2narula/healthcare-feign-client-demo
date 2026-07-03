package com.openfeign.appointment_service.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

/**
 * ================================================================
 * DTO — DoctorResponse (in appointment-service)
 * ================================================================
 * This DTO represents the data appointment-service receives from
 * doctor-service via Feign's DoctorClient.
 *
 * ── HOW FEIGN USES THIS ─────────────────────────────────────────
 * When DoctorClient.getDoctorById(1L) is called:
 *   1. Feign sends: GET http://localhost:8082/api/doctors/1
 *   2. doctor-service responds with JSON:
 *        {"id":1,"name":"Dr. Sarah Mitchell","specialization":"Cardiologist",...}
 *   3. Feign's Jackson decoder deserializes that JSON into this class
 *   4. The DoctorResponse object is returned to AppointmentService
 *
 * ── KEY INSIGHT — DOES NOT NEED TO MATCH EXACTLY ────────────────
 * This DTO does NOT need to be identical to doctor-service's Doctor model.
 * Jackson by default IGNORES any JSON fields not present in this DTO.
 * So if doctor-service adds a new field tomorrow, this class still works.
 *
 * We only include fields that appointment-service actually needs.
 * This is the "consumer-driven contract" approach.
 * ================================================================
 */

@Data
@Builder
@NoArgsConstructor  // required by Jackson for deserialization
@AllArgsConstructor
public class DoctorResponse {

    private Long id;
    private String name;
    private String specialization;
    private String email;
    private String phone;

    // Used in AppointmentService.createAppointment() to validate
    // that the doctor can accept new appointments before booking.
    private boolean available;

    private List<String> availableDays;
    private int experienceYears;
}