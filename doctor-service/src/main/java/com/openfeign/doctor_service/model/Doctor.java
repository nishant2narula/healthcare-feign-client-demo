package com.openfeign.doctor_service.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * ================================================================
 * DOMAIN MODEL — Doctor
 * ================================================================
 * Represents a Doctor in the healthcare system.
 *
 * Here we keep it a plain Java object (POJO) and store instances
 * in a ConcurrentHashMap to keep the focus on Feign, not JPA.
 *
 * LOMBOK ANNOTATIONS explained:
 *   @Data           → generates getters, setters, equals(),
 *                     hashCode(), and toString() at compile time.
 *                     Saves hundreds of lines of boilerplate.
 *
 *   @Builder        → generates a fluent builder API so you can write:
 *                     Doctor.builder().name("Dr. Smith").available(true).build()
 *                     instead of using constructors directly.
 *
 *   @NoArgsConstructor → generates a no-args constructor.
 *                        Required by Jackson when deserializing JSON
 *                        back to a Java object (e.g. in Feign decoder).
 *
 *   @AllArgsConstructor → generates a constructor with all fields as params.
 *                         Required by @Builder to work correctly.
 * ================================================================
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Doctor {

    private Long id;
    private String name;
    private String specialization;
    private String email;
    private String phone;
    private boolean available;
    private List<String> availableDays;
    private int experienceYears;

}
