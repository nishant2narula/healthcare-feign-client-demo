package com.openfeign.doctor_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ================================================================
 * DOCTOR SERVICE — Entry Point
 * ================================================================
 * Port: 8082
 *
 * This is a standalone REST microservice that manages Doctor data.
 * It does NOT use Feign — it simply exposes REST endpoints that
 * the Appointment Service will call via Feign Client.
 *
 * @SpringBootApplication is a shortcut for three annotations:
 *   @Configuration     → marks this class as a source of bean definitions
 *   @EnableAutoConfiguration → tells Spring Boot to auto-configure beans
 *                              based on the jars on the classpath
 *   @ComponentScan     → scans this package and sub-packages for
 *                        @Component, @Service, @Controller etc.
 * ================================================================
 */

@SpringBootApplication
public class DoctorServiceApplication {

	public static void main(String[] args) {

		// SpringApplication.run() bootstraps the Spring Application Context,
		// starts the embedded Tomcat server on port 8082 (from application.yml),
		// and registers all beans found during component scan.

		SpringApplication.run(DoctorServiceApplication.class, args);
	}

}
