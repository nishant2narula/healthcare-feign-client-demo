package com.openfeign.appointment_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * ================================================================
 * DOMAIN EXCEPTIONS — ServiceExceptions
 * ================================================================
 * Custom runtime exceptions thrown by HealthcareErrorDecoder
 * when Feign receives non-2xx responses from downstream services.
 *
 * WHY custom exceptions instead of using FeignException directly?
 *   1. Your service layer can catch specific exception types:
 *        catch (ResourceNotFoundException e) { return empty result; }
 *   2. @ResponseStatus maps exceptions directly to HTTP status codes
 *      when they propagate to the controller — no @ExceptionHandler needed.
 *   3. Cleaner code — meaningful names instead of checking status codes.
 *
 * @ResponseStatus(HttpStatus.NOT_FOUND) means:
 *   If this exception propagates all the way to Spring's DispatcherServlet,
 *   Spring automatically returns HTTP 404 to the client.
 *   (Without this, unhandled RuntimeExceptions return HTTP 500.)
 *
 * ALL classes are package-private (no 'public') intentionally —
 * they are implementation details of this package, only used by
 * HealthcareErrorDecoder and caught within this service.
 * ================================================================
 */

// Thrown when a downstream service returns HTTP 404 Not Found.
// E.g., requested doctorId=999 but no doctor with that ID exists.
@ResponseStatus(HttpStatus.NOT_FOUND)
class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);  // message appears in the error response body
    }
}

// Thrown when a downstream service returns HTTP 400 Bad Request.
// E.g., we sent malformed data in the request body.
@ResponseStatus(HttpStatus.BAD_REQUEST)
class BadRequestException extends RuntimeException {
    public BadRequestException(String message) {
        super(message);
    }
}

// Thrown when a downstream service returns HTTP 401 or 403.
// Check that AuthHeaderInterceptor is providing a valid token.
@ResponseStatus(HttpStatus.UNAUTHORIZED)
class UnauthorizedException extends RuntimeException {
    public UnauthorizedException(String message) {
        super(message);
    }
}

// Thrown for any other unexpected communication failure.
// E.g., unexpected 4xx status codes not covered by the cases above.
@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
class ServiceCommunicationException extends RuntimeException {
    public ServiceCommunicationException(String message) {
        super(message);
    }
}