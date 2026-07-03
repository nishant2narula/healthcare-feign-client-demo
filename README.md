# healthcare-feign-client-demo
A Spring Boot microservices demo showcasing every major OpenFeign feature — declarative clients, request interceptors, custom error decoding, and Resilience4j circuit breaker fallbacks through a healthcare domain. 4 services (API Gateway, Appointment, Doctor, Patient) with in-memory data.

# What This Demonstrates
Declarative Feign client interfaces (@FeignClient)
Request interceptors (auth headers, trace ID propagation)
Custom error decoding (HTTP status → domain exceptions)
Circuit breaker + fallback (Resilience4j)
API Gateway routing (Spring Cloud Gateway)
Full request/response logging (Logger.Level.FULL)

