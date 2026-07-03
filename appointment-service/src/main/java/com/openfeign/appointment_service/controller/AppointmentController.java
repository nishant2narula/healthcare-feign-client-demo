package com.openfeign.appointment_service.controller;

import com.openfeign.appointment_service.dto.AppointmentRequest;
import com.openfeign.appointment_service.dto.AppointmentResponse;
import com.openfeign.appointment_service.dto.DoctorResponse;
import com.openfeign.appointment_service.dto.PatientResponse;
import com.openfeign.appointment_service.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ================================================================
 * REST CONTROLLER — AppointmentController
 * ================================================================
 * Exposes the appointment booking API to external clients (via gateway).
 *
 * WHICH ENDPOINTS TRIGGER FEIGN CALLS:
 *   GET  /api/appointments           → 2 Feign calls PER appointment
 *   GET  /api/appointments/{id}      → 2 Feign calls (1 doctor + 1 patient)
 *   POST /api/appointments           → 2 Feign calls (validate + enrich)
 *   PATCH /api/appointments/{id}/cancel → 2 Feign calls
 *   GET  /api/appointments/doctors/available → 1 Feign call to DoctorClient
 *   GET  /api/appointments/patients  → 1 Feign call to PatientClient
 *
 * The controller itself is thin — it delegates immediately to AppointmentService.
 * All Feign logic is in the service layer, which is the correct design.
 * ================================================================
 */

@Slf4j
@RestController
@RequestMapping("/api/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService appointmentService;

    /**
     * GET /api/appointments
     * Returns all appointments, each enriched with doctor + patient data via Feign.
     * If n appointments exist, this triggers 2n Feign HTTP calls.
     */
    @GetMapping
    public ResponseEntity<List<AppointmentResponse>> getAllAppointments() {
        log.info("GET /api/appointments");
        return ResponseEntity.ok(appointmentService.getAllAppointments());
    }

    /**
     * GET /api/appointments/{id}
     * Returns a single appointment enriched with doctor + patient data.
     *
     * HOW TO OBSERVE FEIGN IN ACTION:
     *   1. Call this endpoint
     *   2. Watch the appointment-service terminal
     *   3. You'll see (with logger-level: FULL):
     *      [DoctorClient#getDoctorById] ---> GET http://localhost:8082/api/doctors/1
     *      [DoctorClient#getDoctorById] Authorization: Bearer demo-secret-token...
     *      [DoctorClient#getDoctorById] X-Trace-Id: abc123...
     *      [DoctorClient#getDoctorById] <--- HTTP/1.1 200 OK (42ms)
     *      [DoctorClient#getDoctorById] {"id":1,"name":"Dr. Sarah Mitchell",...}
     *      [PatientClient#getPatientById] ---> GET http://localhost:8083/api/patients/1
     *      ... (similar output)
     */
    @GetMapping("/{id}")
    public ResponseEntity<AppointmentResponse> getAppointmentById(@PathVariable Long id) {
        log.info("GET /api/appointments/{}", id);
        return appointmentService.getAppointmentById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/appointments
     * Books a new appointment.
     *
     * @Valid validates the request body before entering this method.
     * If doctorId is null, or appointmentDateTime is in the past,
     * Spring returns HTTP 400 automatically.
     *
     * FEIGN CALLS INSIDE:
     *   1. DoctorClient.getDoctorById(doctorId) — validates doctor exists + available
     *   2. PatientClient.getPatientById(patientId) — validates patient exists
     *
     * Returns HTTP 201 Created with the full enriched AppointmentResponse.
     */
    @PostMapping
    public ResponseEntity<AppointmentResponse> createAppointment(
            @Valid @RequestBody AppointmentRequest request) {
        log.info("POST /api/appointments - doctorId={}, patientId={}",
                request.getDoctorId(), request.getPatientId());
        AppointmentResponse response = appointmentService.createAppointment(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PATCH /api/appointments/{id}/cancel
     * Cancels an existing appointment.
     *
     * Uses PATCH (partial update) rather than PUT (full update) because
     * we're only changing the status field, not replacing the entire resource.
     */
    @PatchMapping("/{id}/cancel")
    public ResponseEntity<AppointmentResponse> cancelAppointment(@PathVariable Long id) {
        log.info("PATCH /api/appointments/{}/cancel", id);
        return appointmentService.cancelAppointment(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * GET /api/appointments/doctors/available
     * Returns available doctors from doctor-service via DoctorClient.
     *
     * ── BEST ENDPOINT FOR TESTING CIRCUIT BREAKER ────────────────
     * Step 1: Call this — see real doctor data returned
     * Step 2: Stop doctor-service (Ctrl+C in terminal 1)
     * Step 3: Call this a few times — watch HealthcareErrorDecoder
     *         log errors, Retryer attempt retries, circuit open
     * Step 4: See DoctorClientFallback.getAvailableDoctors() return []
     *         and log: "[Fallback] DoctorClient.getAvailableDoctors() invoked"
     * Step 5: Restart doctor-service
     * Step 6: After 10 seconds (wait-duration-in-open-state), circuit closes
     * Step 7: Call this again — real doctor data returns
     */
    @GetMapping("/doctors/available")
    public ResponseEntity<List<DoctorResponse>> getAvailableDoctors() {
        log.info("GET /api/appointments/doctors/available → triggering DoctorClient Feign call");
        return ResponseEntity.ok(appointmentService.getAvailableDoctors());
    }

    /**
     * GET /api/appointments/patients
     * Returns all patients from patient-service via PatientClient.
     * Stop patient-service to see PatientClientFallback return [].
     */
    @GetMapping("/patients")
    public ResponseEntity<List<PatientResponse>> getAllPatients() {
        log.info("GET /api/appointments/patients → triggering PatientClient Feign call");
        return ResponseEntity.ok(appointmentService.getAllPatients());
    }

    /**
     * GET /api/appointments/health
     * Quick health check showing which Feign clients and circuit breakers
     * are configured. For detailed circuit breaker status, use:
     *   GET http://localhost:8081/actuator/health
     *   GET http://localhost:8081/actuator/circuitbreakers
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "service", "appointment-service",
                "status", "UP",
                "feignClients", "DoctorClient → :8082, PatientClient → :8083",
                "circuitBreakers", "doctor-service (CLOSED), patient-service (CLOSED)"
        ));
    }
}