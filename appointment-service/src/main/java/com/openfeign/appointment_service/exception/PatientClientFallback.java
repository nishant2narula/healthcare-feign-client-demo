package com.openfeign.appointment_service.exception;

import com.openfeign.appointment_service.client.PatientClient;
import com.openfeign.appointment_service.dto.PatientResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * ================================================================
 * CIRCUIT BREAKER FALLBACK — PatientClientFallback
 * ================================================================
 * Identical pattern to DoctorClientFallback, but for PatientClient.
 *
 * KEY INSIGHT — Independent circuit breakers:
 *   Each @FeignClient has its OWN circuit breaker instance, named
 *   after the 'name' attribute ("patient-service").
 *
 *   This means:
 *     - If patient-service goes down: PatientClientFallback is called,
 *       but DoctorClient continues making real HTTP calls successfully.
 *     - If doctor-service goes down: DoctorClientFallback is called,
 *       but PatientClient continues working.
 *
 *   The services are isolated — one failure doesn't cascade to the other.
 *   This is the "bulkhead" resilience pattern.
 *
 * ── TO TEST THIS ────────────────────────────────────────────────
 *   1. Stop patient-service (Ctrl+C in its terminal)
 *   2. Call: GET /api/appointments/1
 *   3. Doctor data loads fine (real HTTP call to doctor-service)
 *   4. Patient data shows "temporarily unavailable" (this fallback)
 *   5. Restart patient-service → normal behavior resumes after 10s
 * ================================================================
 */

@Slf4j
@Component
public class PatientClientFallback implements PatientClient {

    /**
     * Fallback for getPatientById() when patient-service is unreachable.
     * Returns a stub with a clear "unavailable" message.
     * The appointment response is still returned, just with partial patient data.
     */
    @Override
    public PatientResponse getPatientById(Long id) {
        log.warn("[Fallback] PatientClient.getPatientById({}) invoked — " +
                "patient-service is down or circuit is OPEN.", id);

        return PatientResponse.builder()
                .id(id)
                .name("Patient information temporarily unavailable")
                .email("unavailable@system.com")
                .build();
    }

    /**
     * Fallback for getAllPatients() — returns empty list rather than crashing.
     */
    @Override
    public List<PatientResponse> getAllPatients() {
        log.warn("[Fallback] PatientClient.getAllPatients() invoked — " +
                "circuit OPEN, returning empty list.");
        return Collections.emptyList();
    }
}