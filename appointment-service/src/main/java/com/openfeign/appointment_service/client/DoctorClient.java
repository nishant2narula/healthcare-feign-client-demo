package com.openfeign.appointment_service.client;

import com.openfeign.appointment_service.dto.DoctorResponse;
import com.openfeign.appointment_service.exception.DoctorClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * ================================================================
 * FEIGN CLIENT INTERFACE — DoctorClient
 * ================================================================
 * THIS IS THE HEART OF FEIGN.
 * <p>
 * This is just an interface — you write zero implementation code.
 * At application startup, Feign reads this interface, inspects every
 * method's annotations, and generates a full HTTP client proxy automatically.
 * <p>
 * ── @FeignClient ATTRIBUTES ──────────────────────────────────────
 * <p>
 * name = "doctor-service"
 * The logical name of this client.
 * Used as:
 * a) The circuit breaker instance name (must match resilience4j
 * config key in application.yml → resilience4j.circuitbreaker.instances)
 * b) The Spring bean qualifier name
 * c) The service ID for Eureka/Consul if using service discovery
 * <p>
 * url = "${services.doctor-url}"
 * The base URL, resolved from application.yml:
 * services:
 * doctor-url: http://localhost:8082
 * In a production setup with Eureka, remove this attribute and
 * Feign uses 'name' to look up the real host from the service registry.
 * <p>
 * fallback = DoctorClientFallback.class
 * The class to call instead of the real HTTP client when:
 * - The circuit breaker is OPEN (too many failures)
 * - An exception propagates out of the Feign call
 * Requires spring.cloud.openfeign.circuitbreaker.enabled=true in yml.
 * <p>
 * ── HOW SPRING MVC ANNOTATIONS WORK HERE ────────────────────────
 *
 * @GetMapping, @PathVariable etc. are the SAME annotations you use
 * in controllers. Feign's SpringMvcContract parses them to build
 * the HTTP request template for each method.
 * <p>
 * ── FULL EXECUTION FLOW FOR getDoctorById(1L) ───────────────────
 * 1. Your code calls: doctorClient.getDoctorById(1L)
 * 2. JDK Proxy intercepts → finds SynchronousMethodHandler for this method
 * 3. RequestTemplate is built: GET /api/doctors/1
 * 4. AuthHeaderInterceptor.apply() → adds Authorization: Bearer ...
 * 5. TraceIdInterceptor.apply()   → adds X-Trace-Id: abc123
 * 6. LoadBalancer resolves "doctor-service" → http://localhost:8082
 * 7. HTTP GET sent to http://localhost:8082/api/doctors/1
 * 8. Response 200 OK with JSON body
 * 9. Jackson decodes JSON → DoctorResponse object
 * 10. Returns DoctorResponse to your code
 * ================================================================
 */

@FeignClient(
        name = "doctor-service",
        url = "${services.doctor-url}",
        fallback = DoctorClientFallback.class
)
public interface DoctorClient {

    /**
     * Maps to: GET http://localhost:8082/api/doctors/{id}
     *
     * @PathVariable("id") — the "id" name must explicitly match the
     * placeholder name in the URL. Feign requires the explicit name
     * (unlike Spring MVC where it can be inferred from parameter name).
     * <p>
     * Return type DoctorResponse — Feign's Jackson decoder deserializes
     * the JSON response body into this DTO automatically.
     */

    @GetMapping("/api/doctors/{id}")
    DoctorResponse getDoctorById(@PathVariable("id") Long id);

    /**
     * Maps to: GET http://localhost:8082/api/doctors/available
     * Returns list of doctors with available=true.
     * Used to check which doctors can be booked for appointments.
     */
    @GetMapping("/api/doctors/available")
    List<DoctorResponse> getAvailableDoctors();

    /**
     * Maps to: GET http://localhost:8082/api/doctors
     * Returns all doctors regardless of availability.
     */

    @GetMapping("/api/doctors")
    List<DoctorResponse> getAllDoctors();


}
