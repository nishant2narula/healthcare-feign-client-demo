package com.openfeign.appointment_service.interceptor;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import java.util.UUID;

/**
 * ================================================================
 * REQUEST INTERCEPTOR #2 — Distributed Trace ID Propagation
 * ================================================================
 * Propagates a trace ID from the incoming request through to all
 * downstream Feign calls, enabling end-to-end request tracing.
 *
 * ── WHY TRACE IDs MATTER ────────────────────────────────────────
 * Without trace IDs, debugging is painful:
 *   "User reported error at 3pm, where do I even look?"
 *
 * With trace IDs, you can filter logs across ALL services by one ID:
 *   grep "traceId=abc123" *.log
 *   → Shows every log line from appointment-service, doctor-service,
 *     AND patient-service that were part of the same user request.
 *
 * ── MDC (Mapped Diagnostic Context) ────────────────────────────
 * MDC is SLF4J's thread-local key-value store.
 * Any value put in MDC automatically appears in every log line
 * for that thread (if your logback pattern includes %X{traceId}).
 *
 * HOW IT FLOWS:
 *   1. Client sends request with X-Trace-Id: abc123 header
 *   2. TraceIdFilter (servlet filter) puts abc123 into MDC
 *   3. Log lines in AppointmentService say "traceId=abc123"
 *   4. THIS interceptor reads abc123 from MDC
 *   5. Puts X-Trace-Id: abc123 on the outgoing Feign request header
 *   6. DoctorController receives X-Trace-Id: abc123
 *   7. (If doctor-service also had this filter, its logs would too)
 *
 * ── @Order(2) ───────────────────────────────────────────────────
 * Controls execution order among multiple interceptors.
 * @Order(1) = runs first (AuthHeaderInterceptor)
 * @Order(2) = runs second (this class)
 * Lower number = higher priority.
 * ================================================================
 */

@Slf4j
@Component
@Order(2)
public class TraceIdInterceptor implements RequestInterceptor {

    // Header name for the outgoing Feign request — doctor/patient service reads this
    public static final String TRACE_ID_HEADER = "X-Trace-Id";

    // Alternate header name for correlation — same value, different name
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    // Header identifying the originating service — useful for debugging
    public static final String SOURCE_SERVICE_HEADER = "X-Source-Service";

    // Key used to store the trace ID in MDC — must match TraceIdFilter's key
    public static final String MDC_TRACE_KEY = "traceId";

    @Override
    public void apply(RequestTemplate template) {

        // Read trace ID that was set by TraceIdFilter when the incoming request arrived.
        // MDC.get() returns null if the key doesn't exist.
        String traceId = MDC.get(MDC_TRACE_KEY);
        // If no trace ID exists (e.g., this call comes from a scheduler or startup task
        // rather than an HTTP request), generate a fresh one so calls are still traceable.
        if (traceId == null || traceId.isBlank()) {
            traceId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            MDC.put(MDC_TRACE_KEY, traceId);
            log.debug("[TraceInterceptor] No existing traceId found, generated new: {}", traceId);
        }
        log.debug("[TraceInterceptor] Propagating traceId={} → {} {}",
                traceId, template.method(), template.url());
        // Stamp the trace ID into the outgoing request headers.
        // These are visible in Feign's FULL logger output.
        template.header(TRACE_ID_HEADER, traceId);
        template.header(CORRELATION_ID_HEADER, traceId);
        template.header(SOURCE_SERVICE_HEADER, "appointment-service");
    }

}
