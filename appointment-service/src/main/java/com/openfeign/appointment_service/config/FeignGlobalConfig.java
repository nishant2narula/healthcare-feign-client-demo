package com.openfeign.appointment_service.config;

import com.openfeign.appointment_service.exception.HealthcareErrorDecoder;
import feign.Logger;
import feign.Retryer;
import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

/**
 * ================================================================
 * GLOBAL FEIGN CONFIGURATION
 * ================================================================
 * Defines beans that customize Feign behavior for ALL clients in
 * this application context (DoctorClient + PatientClient).
 * <p>
 * ── WHY JAVA CONFIG AND NOT JUST application.yml? ───────────────
 * application.yml can configure: timeouts, logger level, retryer class.
 * Java config can configure: custom implementations of ErrorDecoder,
 * Retryer, Encoder, Decoder, and Logger — things that need code.
 * Both work together: yml sets the values, Java config sets the behavior.
 * <p>
 * ── SCOPE: GLOBAL vs PER-CLIENT ─────────────────────────────────
 * Beans here → apply to ALL Feign clients.
 * For per-client config, create a separate class (no @Configuration
 * or @Component annotation) and reference it in @FeignClient:
 *
 * @FeignClient(name="x", configuration = XConfig.class)
 * WARNING: If you accidentally annotate a per-client config with
 * @Configuration, Spring picks it up globally and it applies everywhere.
 * <p>
 * ── REQUEST INTERCEPTORS ─────────────────────────────────────────
 * AuthHeaderInterceptor and TraceIdInterceptor are @Component beans —
 * Spring auto-registers them with ALL Feign clients without listing
 * them here. That's why they don't appear in this config class.
 * ================================================================
 */

@Configuration
public class FeignGlobalConfig {

    /**
     * ── FEIGN LOGGER LEVEL ───────────────────────────────────────
     * Controls HOW MUCH Feign captures in its internal logging.
     * This works as a PAIR with the SLF4J level in application.yml.
     * <p>
     * Feign Level:         What gets captured internally
     * NONE         →    Nothing
     * BASIC        →    HTTP method, URL, status code, response time
     * HEADERS      →    BASIC + request & response headers
     * FULL         →    HEADERS + request & response body + metadata
     * <p>
     * SLF4J Level (in application.yml):
     * logging.level.com.healthcare.appointment.client.DoctorClient: DEBUG
     * → Controls whether captured logs are WRITTEN to the console/file.
     * Must be DEBUG. If set to INFO or above, Feign logs are silently dropped.
     * <p>
     * BOTH must be set. Missing either one = no Feign logs visible.
     * <p>
     * For production: use BASIC (lower overhead, no sensitive data in logs).
     * For development/demo: use FULL (see complete request/response).
     */
    @Bean
    public Logger.Level feignLoggerLevel() {
        return Logger.Level.FULL;   // change to BASIC for production
    }

    /**
     * ── RETRYER — Exponential Backoff ────────────────────────────
     * Feign's Retryer is consulted after an IOException (network error)
     * or after ErrorDecoder throws a RetryableException.
     * <p>
     * Retryer.Default parameters:
     * period      = 100ms  → wait 100ms before first retry
     * maxPeriod   = 1000ms → never wait more than 1 second between retries
     * maxAttempts = 3      → 1 original attempt + 2 retries = 3 total
     * <p>
     * Backoff calculation: nextWait = min(period * 1.5^attempt, maxPeriod)
     * Attempt 1 fails → wait 100ms
     * Attempt 2 fails → wait 150ms  (100 * 1.5^1)
     * Attempt 3 fails → throw RetryableException to caller
     * <p>
     * IMPORTANT: Without this bean, the default is Retryer.NEVER_RETRY,
     * which means Feign throws immediately on any IOException.
     * <p>
     * CAUTION: Only use retry for IDEMPOTENT operations (GET, DELETE, PUT).
     * Retrying a POST may create duplicate records!
     */
    @Bean
    public Retryer feignRetryer() {
        return new Retryer.Default(
                100,                              // initial retry interval: 100ms
                TimeUnit.SECONDS.toMillis(1),     // max retry interval: 1 second
                3                                 // max total attempts
        );
    }

    /**
     * ── CUSTOM ERROR DECODER ─────────────────────────────────────
     * Replaces Feign's default ErrorDecoder with our custom one.
     * The default would throw a generic FeignException for all errors.
     * Our decoder maps each HTTP status to a meaningful domain exception.
     * <p>
     * "appointment-service" is the label used in error log messages,
     * helping identify which service's Feign client caused the error.
     * <p>
     * This is a global bean — applies to both DoctorClient and PatientClient.
     * The clientName "appointment-service" identifies the calling service,
     * not the target. To differentiate per-client, you'd register separate
     * ErrorDecoder beans in per-client configuration classes instead.
     */
    @Bean
    public ErrorDecoder errorDecoder() {
        return new HealthcareErrorDecoder("appointment-service");
    }
}