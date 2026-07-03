package com.openfeign.appointment_service.client;


import com.openfeign.appointment_service.dto.PatientResponse;
import com.openfeign.appointment_service.exception.PatientClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * ================================================================
 * FEIGN CLIENT INTERFACE — PatientClient
 * ================================================================
 * Same pattern as DoctorClient — targets patient-service on port 8083.
 * <p>
 * KEY POINT — Two independent Feign clients:
 * DoctorClient and PatientClient are completely independent.
 * Each has its own:
 * - Circuit breaker instance (doctor-service vs patient-service)
 * - Connection pool
 * - Timeout configuration (see application.yml feign.client.config)
 * - Fallback class
 * <p>
 * If patient-service crashes:
 * - PatientClient → circuit opens → PatientClientFallback called
 * - DoctorClient → completely unaffected, continues working normally
 * This is the "bulkhead" resilience pattern in microservices.
 * ================================================================
 */

@FeignClient(
        name = "patient-service",
        url = "${services.patient-url}",
        fallback = PatientClientFallback.class
)
public interface PatientClient {

    /**
     * Maps to: GET http://localhost:8083/api/patients/{id}
     * Called when enriching an appointment with patient details.
     */

    @GetMapping("/api/patients/{id}")
    PatientResponse getPatientById(@PathVariable("id") Long id);

    /**
     * Maps to: GET http://localhost:8083/api/patients
     * Returns all patients — used by the /patients proxy endpoint.
     */

    @GetMapping("/api/patients")
    List<PatientResponse> getAllPatients();


}
