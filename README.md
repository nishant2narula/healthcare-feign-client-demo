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


```
Client → api-gateway (8080)
              │
              ▼
     appointment-service (8081)
        │              │
        ▼              ▼
  doctor-service   patient-service
     (8082)            (8083)
```

## Tech Stack

- **Spring Boot** 3.5.15, **Java** 21
- **Spring Cloud** 2025.0.3 — OpenFeign (`spring-cloud-starter-openfeign`) + Gateway (`spring-cloud-starter-gateway`)
- **Resilience4j** — Circuit Breaker (`spring-cloud-starter-circuitbreaker-resilience4j`)
- **Lombok** — boilerplate reduction
- **In-memory store** — `ConcurrentHashMap` (no DB needed)

## Feign Features Implemented

| Feature | File | Package |
|---|---|---|
| Feign client interfaces | `DoctorClient.java`, `PatientClient.java` | `client/` |
| Auth header interceptor | `AuthHeaderInterceptor.java` | `interceptor/` |
| Trace ID propagation | `TraceIdInterceptor.java`, `TraceIdFilter.java` | `interceptor/` |
| Custom Error Decoder | `HealthcareErrorDecoder.java` | `exception/` |
| Circuit breaker fallbacks | `DoctorClientFallback.java`, `PatientClientFallback.java` | `exception/` |
| Global Feign config | `FeignGlobalConfig.java` | `config/` |
| Domain exceptions | `ServiceExceptions.java` | `exception/` |


```
com.openfeign.appointment_service
├── AppointmentServiceApplication.java   (@EnableFeignClients here)
├── client/
│   ├── DoctorClient.java                (@FeignClient → doctor-service:8082)
│   └── PatientClient.java               (@FeignClient → patient-service:8083)
├── config/
│   └── FeignGlobalConfig.java           (Logger.Level.FULL, Retryer, ErrorDecoder)
├── controller/
│   └── AppointmentController.java
├── dto/
│   ├── AppointmentRequest.java
│   ├── AppointmentResponse.java         (combined doctor+patient+appointment)
│   ├── DoctorResponse.java
│   └── PatientResponse.java
├── exception/
│   ├── HealthcareErrorDecoder.java      (maps 404/429/5xx → domain exceptions)
│   ├── DoctorClientFallback.java        (circuit breaker fallback)
│   ├── PatientClientFallback.java       (circuit breaker fallback)
│   └── ServiceExceptions.java
├── interceptor/
│   ├── AuthHeaderInterceptor.java       (adds Authorization: Bearer header)
│   ├── TraceIdInterceptor.java          (propagates X-Trace-Id via MDC)
│   └── TraceIdFilter.java               (extracts trace ID from incoming request)
├── model/
│   └── AppointmentModels.java
└── service/
    └── AppointmentService.java
```

## License

This is a demo/learning project. Feel free to fork and adapt.
