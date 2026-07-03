package com.openfeign.patient_service.controller;

import com.openfeign.patient_service.dto.PatientRequest;
import com.openfeign.patient_service.model.Patient;
import com.openfeign.patient_service.service.PatientService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ================================================================
 * REST CONTROLLER — PatientController
 * ================================================================
 * Exposes HTTP endpoints for the Patient Service.
 * <p>
 * FEIGN CONNECTION:
 * PatientClient (in appointment-service) calls these endpoints.
 * When AppointmentService calls patientClient.getPatientById(1L),
 * the request arrives at GET /api/patients/1 below.
 * <p>
 * The controller layer is intentionally thin — it only:
 * 1. Receives the HTTP request
 * 2. Validates the request body (via @Valid)
 * 3. Delegates to PatientService
 * 4. Wraps the result in the appropriate ResponseEntity
 * ================================================================
 */

@Slf4j
@RestController
@RequestMapping("/api/patients")
@RequiredArgsConstructor
public class PatientController {

    private final PatientService patientService;

    /**
     * GET /api/patients
     * Returns all patients. Called by PatientClient.getAllPatients().
     */
    @GetMapping
    public ResponseEntity<List<Patient>> getAllPatients() {
        return ResponseEntity.ok(patientService.findAll());
    }

    /**
     * GET /api/patients/{id}
     * Returns a single patient by ID.
     * Called by PatientClient.getPatientById(Long id) via Feign.
     * <p>
     * When Feign calls this and the patient exists → 200 OK with JSON body
     * When patient not found → 404 Not Found → HealthcareErrorDecoder
     * in appointment-service converts that 404 to ResourceNotFoundException.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Patient> getPatientById(@PathVariable Long id) {
        log.info("GET /api/patients/{}", id);
        return patientService.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * POST /api/patients
     * Creates a new patient.
     * Returns HTTP 201 Created with the full patient object (including assigned id).
     */
    @PostMapping
    public ResponseEntity<Patient> createPatient(@Valid @RequestBody PatientRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(patientService.create(request));
    }

    /**
     * PUT /api/patients/{id}
     * Full replacement update of a patient record.
     */
    @PutMapping("/{id}")
    public ResponseEntity<Patient> updatePatient(@PathVariable Long id,
                                                 @Valid @RequestBody PatientRequest request) {
        return patientService.update(id, request)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * DELETE /api/patients/{id}
     * Returns 204 No Content on success, 404 if patient not found.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePatient(@PathVariable Long id) {
        return patientService.delete(id)
                ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }


}
