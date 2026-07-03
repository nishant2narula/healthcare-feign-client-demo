package com.openfeign.appointment_service.service;

import com.openfeign.appointment_service.client.*;
import com.openfeign.appointment_service.dto.AppointmentRequest;
import com.openfeign.appointment_service.dto.AppointmentResponse;
import com.openfeign.appointment_service.dto.DoctorResponse;
import com.openfeign.appointment_service.dto.PatientResponse;
import com.openfeign.appointment_service.model.AppointmentModels;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import lombok.RequiredArgsConstructor;

/**
 * ================================================================
 * SERVICE — AppointmentService
 * ================================================================
 * The core business logic class and the main Feign demonstration point.
 *
 * THIS IS WHERE FEIGN CLIENT CALLS HAPPEN.
 *
 * DoctorClient and PatientClient look like regular injected beans —
 * but they are JDK dynamic proxies created by Feign at startup.
 * Every method call on them transparently makes an HTTP request
 * to the corresponding downstream service.
 *
 * ── DEPENDENCY INJECTION ────────────────────────────────────────
 * @RequiredArgsConstructor generates:
 *   public AppointmentService(DoctorClient doctorClient, PatientClient patientClient) {
 *       this.doctorClient = doctorClient;
 *       this.patientClient = patientClient;
 *   }
 * Spring calls this constructor and injects the Feign proxy beans.
 * From this class's perspective, they are just DoctorClient and
 * PatientClient — the HTTP complexity is fully hidden.
 * ================================================================
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class AppointmentService {

    /**
     * Injected by Spring — actually a JDK dynamic proxy implementing DoctorClient.
     * Calling any method on this triggers the full Feign HTTP pipeline:
     *   proxy → RequestTemplate → interceptors → HTTP → decode/fallback
     */
    private final DoctorClient doctorClient;

    /**
     * Same as above but targets patient-service on port 8083.
     * Has its own independent circuit breaker instance.
     */
    private final PatientClient patientClient;

    // Thread-safe in-memory appointment store: key=appointmentId, value=Appointment
    private final Map<Long, AppointmentModels.Appointment> store = new ConcurrentHashMap<>();

    // Thread-safe auto-incrementing ID generator
    private final AtomicLong idSequence = new AtomicLong(1);

    /**
     * Seeds two appointments on startup.
     * Appointment 1: Alice Johnson (patient 1) with Dr. Sarah Mitchell (doctor 1)
     * Appointment 2: Bob Williams (patient 2) with Dr. James Patel (doctor 2)
     *
     * Note: only IDs are stored, not the full objects.
     * When GET /api/appointments/1 is called, Feign fetches the current
     * doctor and patient data from their respective services in real-time.
     */
    @PostConstruct
    public void seedData() {
        store.put(1L, AppointmentModels.Appointment.builder()
                .id(1L).doctorId(1L).patientId(1L)
                .appointmentDateTime(LocalDateTime.now().plusDays(3))
                .status(AppointmentModels.AppointmentStatus.SCHEDULED)
                .reason("Annual cardiac checkup")
                .notes("Patient reports mild chest discomfort")
                .build());

        store.put(2L, AppointmentModels.Appointment.builder()
                .id(2L).doctorId(2L).patientId(2L)
                .appointmentDateTime(LocalDateTime.now().plusDays(7))
                .status(AppointmentModels.AppointmentStatus.CONFIRMED)
                .reason("Follow-up neurological assessment")
                .notes("Post-medication review")
                .build());

        idSequence.set(3L);  // next new appointment will get id=3
        log.info("Appointment service seeded with {} appointments", store.size());
    }

    /**
     * Books a new appointment.
     *
     * FEIGN CALLS MADE:
     *   1. doctorClient.getDoctorById(request.getDoctorId())
     *      → GET http://localhost:8082/api/doctors/{id}
     *      → Used to validate: does the doctor exist and are they available?
     *
     *   2. patientClient.getPatientById(request.getPatientId())
     *      → GET http://localhost:8083/api/patients/{id}
     *      → Used to validate the patient exists and enrich the response.
     *
     * What happens if doctor-service is down?
     *   → DoctorClientFallback.getDoctorById() is called
     *   → Returns stub with available=false
     *   → The availability check fails → IllegalStateException thrown
     *   → Client gets HTTP 500 with "Doctor ... is not available" message
     */
    public AppointmentResponse createAppointment(AppointmentRequest request) {
        log.info("Creating appointment: doctorId={}, patientId={}",
                request.getDoctorId(), request.getPatientId());

        // ── FEIGN CALL #1 ─────────────────────────────────────────
        // Looks like a local method call but makes HTTP GET to doctor-service.
        // All interceptors run, circuit breaker monitors this call.
        log.debug("Calling DoctorClient.getDoctorById({}) via Feign...", request.getDoctorId());
        DoctorResponse doctor = doctorClient.getDoctorById(request.getDoctorId());

        // Business rule: only book appointments with available doctors
        if (!doctor.isAvailable()) {
            throw new IllegalStateException(
                    "Doctor '" + doctor.getName() + "' is not currently available for appointments");
        }

        // ── FEIGN CALL #2 ─────────────────────────────────────────
        // Validates patient exists. If patient-service is down, fallback stub is returned.
        log.debug("Calling PatientClient.getPatientById({}) via Feign...", request.getPatientId());
        PatientResponse patient = patientClient.getPatientById(request.getPatientId());

        // Create and persist the appointment (only storing IDs, not full objects)
        AppointmentModels.Appointment appointment = AppointmentModels.Appointment.builder()
                .id(idSequence.getAndIncrement())
                .doctorId(request.getDoctorId())
                .patientId(request.getPatientId())
                .appointmentDateTime(request.getAppointmentDateTime())
                .status(AppointmentModels.AppointmentStatus.SCHEDULED)
                .reason(request.getReason())
                .notes(request.getNotes())
                .build();

        store.put(appointment.getId(), appointment);
        log.info("Appointment {} created: patient='{}' with doctor='{}'",
                appointment.getId(), patient.getName(), doctor.getName());

        // Build the rich response combining local + feign-fetched data
        return toResponse(appointment, doctor, patient);
    }

    /**
     * Fetches a single appointment enriched with doctor and patient data.
     *
     * Every call here triggers 2 Feign HTTP requests (unless using cache).
     * You can observe both in the appointment-service console logs:
     *   [DoctorClient#getDoctorById] ---> GET http://localhost:8082/...
     *   [PatientClient#getPatientById] ---> GET http://localhost:8083/...
     */
    public Optional<AppointmentResponse> getAppointmentById(Long id) {
        return Optional.ofNullable(store.get(id))
                .map(appointment -> {
                    log.debug("Enriching appointment id={} with Feign calls", id);

                    // Two independent Feign calls — each has its own circuit breaker.
                    // One failing does not prevent the other from being called.
                    DoctorResponse doctor   = doctorClient.getDoctorById(appointment.getDoctorId());
                    PatientResponse patient = patientClient.getPatientById(appointment.getPatientId());

                    return toResponse(appointment, doctor, patient);
                });
    }

    /**
     * Returns all appointments, each enriched via Feign.
     * If there are 10 appointments, this makes 20 Feign calls (10 doctor + 10 patient).
     * In production, you'd add caching (@Cacheable) or batch fetching to optimize this.
     */
    public List<AppointmentResponse> getAllAppointments() {
        log.debug("Loading {} appointments and enriching with Feign calls", store.size());

        return store.values().stream()
                .map(appointment -> {
                    DoctorResponse doctor   = doctorClient.getDoctorById(appointment.getDoctorId());
                    PatientResponse patient = patientClient.getPatientById(appointment.getPatientId());
                    return toResponse(appointment, doctor, patient);
                })
                .toList();
    }

    /**
     * Cancels an appointment and returns the updated state.
     * Still enriches with Feign calls so the response includes doctor/patient data.
     */
    public Optional<AppointmentResponse> cancelAppointment(Long id) {
        return Optional.ofNullable(store.get(id))
                .map(appointment -> {
                    appointment.setStatus(AppointmentModels.AppointmentStatus.CANCELLED);
                    DoctorResponse doctor   = doctorClient.getDoctorById(appointment.getDoctorId());
                    PatientResponse patient = patientClient.getPatientById(appointment.getPatientId());
                    log.info("Appointment {} cancelled", id);
                    return toResponse(appointment, doctor, patient);
                });
    }

    /**
     * Returns available doctors by delegating directly to DoctorClient.
     * Best endpoint to test the circuit breaker — stop doctor-service
     * and this returns [] from DoctorClientFallback.getAvailableDoctors().
     */
    public List<DoctorResponse> getAvailableDoctors() {
        log.debug("Delegating to DoctorClient.getAvailableDoctors() via Feign");
        return doctorClient.getAvailableDoctors();
    }

    /**
     * Returns all patients by delegating to PatientClient.
     * Demonstrates a list Feign call returning a collection.
     */
    public List<PatientResponse> getAllPatients() {
        log.debug("Delegating to PatientClient.getAllPatients() via Feign");
        return patientClient.getAllPatients();
    }

    /**
     * Private helper — assembles the rich AppointmentResponse from all three data sources.
     * Called after every Feign enrichment to keep the assembly logic in one place.
     *
     * @param a       Local appointment data (from our store)
     * @param doctor  Feign-fetched doctor data (or fallback stub)
     * @param patient Feign-fetched patient data (or fallback stub)
     */
    private AppointmentResponse toResponse(AppointmentModels.Appointment a,
                                           DoctorResponse doctor,
                                           PatientResponse patient) {
        return AppointmentResponse.builder()
                .id(a.getId())
                .status(a.getStatus())
                .appointmentDateTime(a.getAppointmentDateTime())
                .reason(a.getReason())
                .notes(a.getNotes())
                .doctor(doctor)     // enriched from doctor-service via Feign
                .patient(patient)   // enriched from patient-service via Feign
                .build();
    }
}