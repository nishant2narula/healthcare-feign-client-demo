package com.openfeign.appointment_service.interceptor;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * ================================================================
 * SERVLET FILTER — TraceIdFilter
 * ================================================================
 * Runs on EVERY incoming HTTP request to the Appointment Service,
 * before any controller or service code executes.
 *
 * Responsibilities:
 *   1. Extract or generate a trace ID
 *   2. Store it in MDC so all log lines in this thread carry it
 *   3. Echo it back to the caller in the response header
 *   4. ALWAYS clean up MDC when done (critical for thread pool safety)
 *
 * ── OncePerRequestFilter ────────────────────────────────────────
 * Spring's base class that guarantees doFilterInternal() is called
 * EXACTLY ONCE per request, even if the filter is registered multiple
 * times (which can happen with certain servlet container configurations).
 *
 * ── @Order(1) ───────────────────────────────────────────────────
 * This filter runs FIRST among all filters because trace ID setup
 * must happen before any logging occurs.
 *
 * ── THREAD-LOCAL MEMORY LEAK WARNING ────────────────────────────
 * Spring Boot uses a thread pool for handling HTTP requests.
 * Threads are REUSED for multiple requests. If you put something in
 * MDC (which is ThreadLocal-based) and don't clear it, the next
 * request handled by that same thread inherits the old trace ID.
 * The finally block in doFilterInternal() prevents this leak.
 *
 * ── COMPLETE FLOW THROUGH THE SYSTEM ────────────────────────────
 * Incoming: GET /api/appointments/1 with X-Trace-Id: myTrace999
 *
 * TraceIdFilter:
 *   MDC.put("traceId", "myTrace999")
 *   response.setHeader("X-Trace-Id", "myTrace999")
 *       ↓
 * AppointmentController.getAppointmentById(1)
 *   log.info(...) → [traceId=myTrace999] Fetching appointment 1
 *       ↓
 * AppointmentService
 *   doctorClient.getDoctorById(1)
 *       ↓
 * TraceIdInterceptor:
 *   reads MDC → "myTrace999"
 *   stamps X-Trace-Id: myTrace999 on Feign request
 *       ↓
 * HTTP GET to doctor-service with X-Trace-Id: myTrace999
 *       ↓
 * Response returned to client with X-Trace-Id: myTrace999
 *       ↓
 * finally: MDC.clear() — clean up for next request on this thread
 * ================================================================
 */

@Slf4j
@Component
@Order(1)   // runs first — before all other filters
public class TraceIdFilter extends OncePerRequestFilter {

    // The HTTP header name we read from incoming requests
    private static final String TRACE_HEADER = "X-Trace-Id";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        // try-finally ensures MDC.clear() runs even if an exception occurs
        try {
            // Check if the caller (e.g., API Gateway or Postman) already
            // provided a trace ID. This allows end-to-end tracing from
            // the very first hop through all services.
            String traceId = request.getHeader(TRACE_HEADER);

            if (traceId == null || traceId.isBlank()) {
                // No trace ID provided — generate a fresh one.
                // UUID.randomUUID() generates a globally unique 36-char string.
                // We trim it to 16 chars (still very low collision probability).
                traceId = UUID.randomUUID().toString()
                        .replace("-", "")
                        .substring(0, 16);
            }

            // Put in MDC — now every log.info/warn/error in this thread
            // will automatically include [traceId=xxx] in the log line
            // (requires %X{traceId} in your logback pattern).
            MDC.put(TraceIdInterceptor.MDC_TRACE_KEY, traceId);

            // Echo the trace ID back to the caller in the response header.
            // Useful for correlating API responses with server logs.
            response.setHeader(TRACE_HEADER, traceId);

            log.debug("[TraceFilter] {} {} | traceId={}",
                    request.getMethod(), request.getRequestURI(), traceId);

            // Continue the filter chain — passes control to the next filter,
            // and eventually to the controller/service/response.
            filterChain.doFilter(request, response);

        } finally {
            // CRITICAL: Always clear MDC after the request completes.
            // Threads in Spring's thread pool are reused. Without this,
            // the next request on this thread would inherit this trace ID.
            MDC.clear();
        }
    }
}