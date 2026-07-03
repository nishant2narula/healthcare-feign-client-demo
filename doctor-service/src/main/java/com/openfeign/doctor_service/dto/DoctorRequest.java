package com.openfeign.doctor_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


import java.util.List;

/**
 * ================================================================
 * DATA TRANSFER OBJECT (DTO) — DoctorRequest
 * ================================================================
 * Used as the request body for POST /api/doctors and PUT /api/doctors/{id}.
 *
 * WHY a separate DTO instead of using the Doctor model directly?
 *   1. The model has an auto-generated 'id' field — clients shouldn't
 *      send that in the request body.
 *   2. DTOs let you add validation rules without polluting the domain model.
 *   3. DTOs decouple your API contract from your internal data model —
 *      you can change the model without breaking the API.
 *
 * VALIDATION ANNOTATIONS (from jakarta.validation):
 *   @NotBlank   → field must not be null, empty, or whitespace-only
 *   @NotNull    → field must not be null (but can be empty)
 *   @Email      → field must be a valid email format
 *
 * These are enforced by @Valid in the controller method signature.
 * If validation fails, Spring automatically returns HTTP 400 Bad Request.
 * ================================================================
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DoctorRequest {

    @NotBlank(message = "Doctor name is required")
    private String name;

    @NotBlank(message = "Specialization is required")
    private String specialization;

    @Email
    @NotNull
    private String email;

    private String phone;
    private boolean available;
    private List<String> availableDays;
    private int experienceYears;

}
