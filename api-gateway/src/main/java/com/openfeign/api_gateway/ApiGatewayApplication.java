package com.openfeign.api_gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ================================================================
 * API GATEWAY — Entry Point
 * ================================================================
 * Port: 8080 — the single external entry point for all services.
 *
 * Built on Spring Cloud Gateway, which uses WebFlux (reactive/non-blocking)
 * rather than the traditional Spring MVC (servlet/blocking) stack.
 *
 * IMPORTANT: Do NOT add spring-boot-starter-web to this service's pom.xml.
 * spring-cloud-starter-gateway includes WebFlux. Adding spring-boot-starter-web
 * (which includes Tomcat + Spring MVC) causes a startup conflict.
 *
 * WHAT THE GATEWAY DOES:
 *   1. Receives all external HTTP requests on port 8080
 *   2. Matches the request path against route predicates (in application.yml)
 *   3. Applies filters (e.g., adds X-Auth-Token header to every request)
 *   4. Forwards the request to the appropriate downstream service
 *   5. Returns the downstream response to the client
 *
 * ROUTES (defined in application.yml):
 *   /api/appointments/** → http://localhost:8081 (appointment-service)
 *   /api/doctors/**     → http://localhost:8082 (doctor-service)
 *   /api/patients/**    → http://localhost:8083 (patient-service)
 *
 * Every forwarded request gets:
 *   X-Auth-Token: gateway-token-healthcare-2024
 *   X-Gateway-Source: api-gateway
 *
 * You can see these headers in the Feign FULL logger output when
 * requests arrive at appointment-service.
 *
 * No Java code is needed for routing — everything is in application.yml.
 * This main class is just the Spring Boot entry point.
 * ================================================================
 */

@SpringBootApplication
public class ApiGatewayApplication {

	public static void main(String[] args) {

		SpringApplication.run(ApiGatewayApplication.class, args);
	}

}
