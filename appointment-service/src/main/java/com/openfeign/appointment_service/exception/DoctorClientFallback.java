package com.openfeign.appointment_service.exception;

import com.openfeign.appointment_service.client.DoctorClient;
import com.openfeign.appointment_service.dto.DoctorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

/**
 * ================================================================
 * CIRCUIT BREAKER FALLBACK — DoctorClientFallback
 * ================================================================
 * Provides "graceful degradation" responses when the DoctorClient
 * circuit breaker is OPEN or a call fails.
 *
 * ── WHEN IS THIS CALLED? ────────────────────────────────────────
 *   a) Circuit is OPEN:
 *      doctor-service failed enough times (failure-rate-threshold: 50%)
 *      that Resilience4j stopped trying. All calls go here immediately.
 *
 *   b) Circuit is HALF-OPEN:
 *      A probe call was sent to check if doctor-service recovered.
 *      If it failed again, this fallback is called.
 *
 *   c) Exception during call:
 *      Any exception (from ErrorDecoder or network) that isn't handled
 *      by the Retryer propagates to the circuit breaker, which then
 *      calls this fallback.
 *
 * ── CIRCUIT BREAKER STATE MACHINE ───────────────────────────────
 *
 *   CLOSED (normal)                 OPEN (failures exceeded threshold)
 *     │                               │
 *     │ calls go to real service      │ calls go to this fallback
 *     │                               │
 *     └──[failure rate ≥ 50%]────────►│
 *                                     │
 *     ◄──[probe calls succeed]────────┤ [wait 10s]
 *     │                               │
 *   HALF-OPEN                         ▼
 *     │ sends probe calls         OPEN (again if probes fail)
 *
 * ── REQUIREMENTS ────────────────────────────────────────────────
 *   1. Must implement the SAME interface as the Feign client (DoctorClient)
 *   2. Must be a @Component so Spring can find and wire it
 *   3. Referenced in @FeignClient(fallback = DoctorClientFallback.class)
 *   4. Requires spring.cloud.openfeign.circuitbreaker.enabled=true in yml
 *
 * ── FALLBACK STRATEGY ───────────────────────────────────────────
 *   For single-item lookups: return a stub object with "unavailable" message
 *     → Appointment response is still returned, just with partial data.
 *   For list operations: return empty list
 *     → No crash, caller handles empty collection gracefully.
 * ================================================================
 */

@Slf4j
@Component
public class DoctorClientFallback implements DoctorClient {

    /**
     * Fallback for getDoctorById() — returns a stub doctor object.
     * The appointment response will still be returned to the caller,
     * but with doctor name = "Doctor information temporarily unavailable".
     *
     * @param id The doctor ID that was requested (passed through to fallback)
     */
    @Override
    public DoctorResponse getDoctorById(Long id) {
        log.warn("[Fallback] DoctorClient.getDoctorById({}) invoked — " +
                "circuit is OPEN or call failed. Returning stub response.", id);

        // Return a stub DoctorResponse — the appointment booking flow
        // won't crash, but the UI should show a "data unavailable" indicator.
        return DoctorResponse.builder()
                .id(id)
                .name("Doctor information temporarily unavailable")
                .specialization("Unknown")
                .available(false)   // marking unavailable prevents new bookings with this stub
                .build();
    }

    /**
     * Fallback for getAvailableDoctors() — returns empty list.
     * The appointment/doctors/available endpoint will return [] instead of crashing.
     * This is the BEST endpoint to test the circuit breaker:
     *   1. Stop doctor-service
     *   2. Call GET /api/appointments/doctors/available a few times
     *   3. Watch logs — circuit opens, this fallback is called
     *   4. Restart doctor-service
     *   5. After 10s, circuit closes, real data returns
     */
    @Override
    public List<DoctorResponse> getAvailableDoctors() {
        log.warn("[Fallback] DoctorClient.getAvailableDoctors() invoked — " +
                "returning empty list. Check if doctor-service is running.");
        return Collections.emptyList();
    }

    /**
     * Fallback for getAllDoctors() — returns empty list.
     */
    @Override
    public List<DoctorResponse> getAllDoctors() {
        log.warn("[Fallback] DoctorClient.getAllDoctors() invoked — " +
                "circuit OPEN, returning empty list.");
        return Collections.emptyList();
    }
}