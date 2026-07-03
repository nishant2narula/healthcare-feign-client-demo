package com.openfeign.patient_service.service;

import com.openfeign.patient_service.dto.PatientRequest;
import com.openfeign.patient_service.model.Patient;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ================================================================
 * SERVICE — PatientService
 * ================================================================
 * Business logic for Patient CRUD operations.
 * Same in-memory store pattern as DoctorService.
 * <p>
 * Pre-seeded with 3 patients that match the appointment seed data:
 * Patient 1 (Alice Johnson)  → paired with Doctor 1 in Appointment 1
 * Patient 2 (Bob Williams)   → paired with Doctor 2 in Appointment 2
 * Patient 3 (Carol Martinez) → available for new appointment bookings
 * ================================================================
 */

@Slf4j
@Service
public class PatientService {

    // Thread-safe in-memory store: key=patientId, value=Patient
    private final Map<Long, Patient> store = new ConcurrentHashMap<>();

    // Thread-safe auto-incrementing ID generator
    private final AtomicLong idSequence = new AtomicLong(1);

    /**
     * Seeds patient data on application startup.
     * Called automatically by Spring after bean initialization.
     * Medical histories are realistic — useful when demoing the full
     * AppointmentResponse which includes patient details fetched via Feign.
     */
    @PostConstruct
    public void seedData() {
        save(Patient.builder()
                .id(idSequence.getAndIncrement())
                .name("Alice Johnson").email("alice.johnson@email.com")
                .phone("+1-555-1001").dateOfBirth(LocalDate.of(1985, 3, 15))
                .bloodGroup("A+").address("123 Maple Street, Springfield")
                .medicalHistory(List.of("Hypertension", "Seasonal Allergies"))
                .insuranceProvider("BlueCross").insurancePolicyNumber("BC-2024-001")
                .build());

        save(Patient.builder()
                .id(idSequence.getAndIncrement())
                .name("Bob Williams").email("bob.williams@email.com")
                .phone("+1-555-1002").dateOfBirth(LocalDate.of(1972, 8, 22))
                .bloodGroup("O-").address("456 Oak Avenue, Shelbyville")
                .medicalHistory(List.of("Diabetes Type 2", "High Cholesterol"))
                .insuranceProvider("Aetna").insurancePolicyNumber("AE-2024-002")
                .build());

        save(Patient.builder()
                .id(idSequence.getAndIncrement())
                .name("Carol Martinez").email("carol.martinez@email.com")
                .phone("+1-555-1003").dateOfBirth(LocalDate.of(1990, 11, 5))
                .bloodGroup("B+").address("789 Pine Road, Capital City")
                .medicalHistory(List.of("Asthma"))
                .insuranceProvider("UnitedHealth").insurancePolicyNumber("UH-2024-003")
                .build());

        log.info("Patient service seeded with {} patients", store.size());
    }

    public List<Patient> findAll() {
        return new ArrayList<>(store.values());
    }

    public Optional<Patient> findById(Long id) {
        log.debug("Fetching patient id={}", id);
        return Optional.ofNullable(store.get(id));
    }

    public Patient create(PatientRequest req) {
        Patient p = Patient.builder()
                .id(idSequence.getAndIncrement())
                .name(req.getName()).email(req.getEmail()).phone(req.getPhone())
                .dateOfBirth(req.getDateOfBirth()).bloodGroup(req.getBloodGroup())
                .address(req.getAddress()).medicalHistory(req.getMedicalHistory())
                .insuranceProvider(req.getInsuranceProvider())
                .insurancePolicyNumber(req.getInsurancePolicyNumber())
                .build();
        return save(p);
    }

    public Optional<Patient> update(Long id, PatientRequest req) {
        return findById(id).map(p -> {
            p.setName(req.getName());
            p.setEmail(req.getEmail());
            p.setPhone(req.getPhone());
            p.setDateOfBirth(req.getDateOfBirth());
            p.setBloodGroup(req.getBloodGroup());
            p.setAddress(req.getAddress());
            p.setMedicalHistory(req.getMedicalHistory());
            p.setInsuranceProvider(req.getInsuranceProvider());
            p.setInsurancePolicyNumber(req.getInsurancePolicyNumber());
            return save(p);
        });
    }

    public boolean delete(Long id) {
        return store.remove(id) != null;
    }

    private Patient save(Patient p) {
        store.put(p.getId(), p);
        return p;
    }


}
