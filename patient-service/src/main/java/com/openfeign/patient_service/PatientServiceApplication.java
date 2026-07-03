package com.openfeign.patient_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ================================================================
 * PATIENT SERVICE — Entry Point
 * ================================================================
 * Port: 8083
 *
 * Standalone REST microservice managing Patient data.
 * Exposes endpoints consumed by the Appointment Service via
 * Feign's PatientClient interface.
 *
 * Identical structure to DoctorServiceApplication — each service
 * is independently deployable and has its own Spring context.
 * ================================================================
 */

@SpringBootApplication
public class PatientServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(PatientServiceApplication.class, args);
	}

}
