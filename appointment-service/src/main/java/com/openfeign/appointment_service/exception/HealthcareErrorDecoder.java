package com.openfeign.appointment_service.exception;

import feign.Request;
import feign.Response;
import feign.RetryableException;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;

import java.util.Date;

/**
 * ================================================================
 * CUSTOM ERROR DECODER — HealthcareErrorDecoder
 * ================================================================
 * Feign's default ErrorDecoder wraps every non-2xx response in a
 * generic FeignException. That's not useful — your service layer
 * can't distinguish a 404 from a 503 without inspecting the raw exception.
 *
 * This class replaces the default with smart, domain-aware error handling.
 *
 * ── WHEN IS ErrorDecoder CALLED? ────────────────────────────────
 *   ✅ Called for: 4xx and 5xx HTTP responses
 *   ❌ NOT called for: IOException (network failure) → goes to Retryer
 *   ❌ NOT called for: 2xx responses → goes to Decoder (Jackson)
 *
 * ── POSITION IN THE FEIGN PIPELINE ─────────────────────────────
 *   HTTP Response received
 *       ↓
 *   Is status 2xx? → YES → Jackson Decoder → return Java object
 *                  → NO  → ErrorDecoder.decode() ← WE ARE HERE
 *                              ↓
 *             Returns an Exception (two possible types):
 *               a) Regular exception (RuntimeException etc.)
 *                    → propagates directly to the caller
 *               b) RetryableException
 *                    → goes to Retryer.continueOrPropagate()
 *                    → Retryer decides: retry or throw
 *
 * ── RETRY vs NON-RETRY DECISION ─────────────────────────────────
 *   404 NOT FOUND   → don't retry (resource genuinely missing)
 *   400 BAD REQUEST → don't retry (we sent bad data, retrying won't help)
 *   401/403         → don't retry (auth problem, retrying won't help)
 *   429 RATE LIMITED → retry (server may accept us after a delay)
 *   503 UNAVAILABLE  → retry (server is temporarily overloaded)
 *   5xx OTHER        → retry (server-side transient errors)
 *
 * ── HOW TO REGISTER ─────────────────────────────────────────────
 *   As a @Bean in FeignGlobalConfig → applies to ALL Feign clients.
 *   Or in a per-client @FeignClient(configuration=X.class) → one client only.
 * ================================================================
 */

@Slf4j
public class HealthcareErrorDecoder implements ErrorDecoder {

    private final String clientName;

    public HealthcareErrorDecoder(String clientName) {
        this.clientName = clientName;
    }

    @Override
    public Exception decode(String methodKey, Response response) {

        log.error("[{}] Error response: HTTP {} for method: {}",
                clientName, response.status(), methodKey);

        // Helper variables to avoid repetition in each case
        Request request = response.request();
        Request.HttpMethod httpMethod = request.httpMethod();

        return switch (response.status()) {

            case 404 -> {
                // Resource doesn't exist — retrying will never help.
                // Throw domain exception so the caller can handle 404 specifically.
                log.warn("[{}] Resource not found: {}", clientName, methodKey);
                yield new ResourceNotFoundException(
                        clientName + ": Resource not found → " + methodKey);
            }

            case 400 -> {
                // Bad request — we sent invalid data. Retrying won't fix it.
                log.warn("[{}] Bad request sent to: {}", clientName, methodKey);
                yield new BadRequestException(
                        clientName + ": Bad request → " + methodKey);
            }

            case 401, 403 -> {
                // Auth failure — check AuthHeaderInterceptor is sending the correct token.
                log.error("[{}] Auth error {} — verify Authorization header in interceptor",
                        clientName, response.status());
                yield new UnauthorizedException(
                        clientName + ": Auth error " + response.status() + " → " + methodKey);
            }

            case 429 -> {
                // Rate limited — server is asking us to slow down.
                // RetryableException signals Feign's Retryer to wait before retrying.
                //
                // FIX: Cast null to (Date) explicitly.
                // RetryableException has two constructors in OpenFeign 12+:
                //   (int, String, HttpMethod, Date, Request)  ← we want this one
                //   (int, String, HttpMethod, Long, Request)
                // Bare 'null' is ambiguous. (Date) null resolves the ambiguity.
                //
                // The Date parameter = "retry not before this time".
                // Passing null means: use the Retryer's own backoff schedule.
                log.warn("[{}] Rate limited (HTTP 429) — will retry with backoff", clientName);
                yield new RetryableException(
                        response.status(),
                        "Rate limited by " + clientName + " — retrying after backoff",
                        httpMethod,
                        (Date) null,    // ← THE FIX: explicit cast resolves ambiguous constructor
                        request);
            }

            case 503 -> {
                // Service temporarily unavailable — good candidate for retry.
                log.warn("[{}] Service unavailable (HTTP 503) — marking retryable", clientName);
                yield new RetryableException(
                        response.status(),
                        clientName + " is temporarily unavailable",
                        httpMethod,
                        (Date) null,    // ← same fix here
                        request);
            }

            default -> {
                if (response.status() >= 500) {
                    // Any other 5xx (500, 502, 504 etc.) — transient server error, retry.
                    log.error("[{}] Server error {} — marking retryable for: {}",
                            clientName, response.status(), methodKey);
                    yield new RetryableException(
                            response.status(),
                            clientName + ": Server error " + response.status(),
                            httpMethod,
                            (Date) null,    // ← same fix here
                            request);
                }
                // Unexpected status (e.g., 422) — don't retry, propagate as-is
                log.error("[{}] Unexpected status {} for: {}",
                        clientName, response.status(), methodKey);
                yield new ServiceCommunicationException(
                        clientName + ": Unexpected error " + response.status() + " → " + methodKey);
            }
        };
    }
}