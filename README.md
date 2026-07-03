# healthcare-feign-client-demo
A Spring Boot microservices demo showcasing every major OpenFeign feature — declarative clients, request interceptors, custom error decoding, and Resilience4j circuit breaker fallbacks through a healthcare domain. 4 services (API Gateway, Appointment, Doctor, Patient) with in-memory data.

# What This Demonstrates
Declarative Feign client interfaces (@FeignClient)
Request interceptors (auth headers, trace ID propagation)
Custom error decoding (HTTP status → domain exceptions)
Circuit breaker + fallback (Resilience4j)
API Gateway routing (Spring Cloud Gateway)
Full request/response logging (Logger.Level.FULL)


# Architecture
| Service | Port | Role |
|---|---|---|
| api-gateway | 8080 | Spring Cloud Gateway — routes traffic, adds auth headers |
| appointment-service | 8081 | Core Feign showcase — all Feign features live here |
| doctor-service | 8082 | Downstream REST API — doctors CRUD |
| patient-service | 8083 | Downstream REST API — patient CRUD |
