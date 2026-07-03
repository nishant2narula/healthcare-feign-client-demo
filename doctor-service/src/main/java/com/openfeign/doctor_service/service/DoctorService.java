package com.openfeign.doctor_service.service;

import com.openfeign.doctor_service.dto.DoctorRequest;
import com.openfeign.doctor_service.model.Doctor;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * ================================================================
 * SERVICE — DoctorService
 * ================================================================
 * Business logic layer for Doctor operations.
 * Acts as the intermediary between the controller and the data store.
 *
 * @Service marks this as a Spring-managed service bean.
 * Spring creates one instance (singleton scope by default) and
 * injects it wherever DoctorService is declared as a dependency.
 *
 * @Slf4j (Lombok) generates a static 'log' field of type Logger.
 * Equivalent to writing:
 *   private static final Logger log = LoggerFactory.getLogger(DoctorService.class);
 *
 * DATA STORE CHOICE:
 *   ConcurrentHashMap is used instead of a database to keep this
 *   demo runnable without any external infrastructure.
 *   ConcurrentHashMap (not HashMap) is used because Spring Boot apps
 *   are multi-threaded — multiple HTTP requests may call these methods
 *   simultaneously and HashMap is NOT thread-safe.
 *
 *   AtomicLong for ID generation — thread-safe auto-increment counter.
 *   getAndIncrement() atomically returns current value then increments.
 * ================================================================
 */

@Slf4j
@Service
public class DoctorService {

    // Thread-safe in-memory store: key=doctorId, value=Doctor object
    private final Map<Long, Doctor> store = new ConcurrentHashMap<>();

    // Thread-safe ID counter — starts at 1, increments for each new doctor
    private final AtomicLong idSequence = new AtomicLong(1);


    /**
     * @PostConstruct — Spring calls this method ONCE, immediately after
     * the bean is created and all dependencies are injected.
     * Used here to pre-populate the store with test data so the API
     * is usable the moment the service starts.
     *
     */

    @PostConstruct
    public void addData() {
        save(Doctor.builder()
                .id(idSequence.getAndIncrement())
                .name("Dr. Sarah Mitchell")
                .specialization("Cardiologist")
                .email("sarah.mitchell@hospital.com")
                .phone("+1-555-0101")
                .available(true)
                .availableDays(List.of("MONDAY", "WEDNESDAY", "FRIDAY"))
                .experienceYears(12)
                .build());

        save(Doctor.builder()
                .id(idSequence.getAndIncrement())
                .name("Dr. James Patel")
                .specialization("Neurologist")
                .email("james.patel@hospital.com")
                .phone("+1-555-0102")
                .available(true)
                .availableDays(List.of("TUESDAY", "THURSDAY"))
                .experienceYears(8)
                .build());

        save(Doctor.builder()
                .id(idSequence.getAndIncrement())
                .name("Dr. Emily Chen")
                .specialization("Orthopedic Surgeon")
                .email("emily.chen@hospital.com")
                .phone("+1-555-0103")
                .available(false)
                .availableDays(List.of("MONDAY", "TUESDAY", "WEDNESDAY"))
                .experienceYears(15)
                .build());

        log.info("Doctor service seeded with {} doctors", store.size());
    }

    /**
     * Returns all doctors as a new ArrayList.
     * We copy store.values() into a new list to prevent callers from
     * holding a reference to the internal map's live collection.
     */

    public List<Doctor> findAll() { return new ArrayList<>(store.values()); }

    /**
     * Returns an Optional<Doctor> — forces the caller to handle the
     * "doctor not found" case explicitly instead of getting a NullPointerException.
     * The controller maps Optional.empty() → HTTP 404.
     */

    public Optional<Doctor> findById(Long id) {
        log.debug("Fetching doctor id={}", id);
        return Optional.ofNullable(store.get(id));
    }

    /**
     * Returns only doctors where available=true.
     * Stream.filter() creates a new stream with only matching elements.
     * .toList() collects to an unmodifiable List (Java 16+).
     * Called by Feign's DoctorClient.getAvailableDoctors().
     */

    public List<Doctor> findAvailable() {
        return store.values().stream().filter(Doctor::isAvailable).toList();
    }

    /**
     * Case-insensitive specialization search.
     * equalsIgnoreCase handles "Cardiologist" == "cardiologist" == "CARDIOLOGIST".
     */

    public List<Doctor> findBySpecialization(String spec) {
        return store.values().stream()
                .filter(d -> d.getSpecialization().equalsIgnoreCase(spec))
                .toList();
    }

    /**
     * Creates a new Doctor from the request DTO, assigns a new ID,
     * saves it to the store, and returns the saved entity.
     */

    public Doctor create(DoctorRequest req) {
        Doctor d = Doctor.builder()
                .id(idSequence.getAndIncrement())
                .name(req.getName()).specialization(req.getSpecialization())
                .email(req.getEmail()).phone(req.getPhone())
                .available(req.isAvailable()).availableDays(req.getAvailableDays())
                .experienceYears(req.getExperienceYears())
                .build();
        return save(d);
    }

    /**
     * Updates an existing Doctor if found, returns Optional.empty() if not.
     * .map() on Optional only executes the lambda if the Optional is non-empty.
     */

    public Optional<Doctor> update(Long id, DoctorRequest req) {
        return findById(id).map(d -> {
            d.setName(req.getName()); d.setSpecialization(req.getSpecialization());
            d.setEmail(req.getEmail()); d.setPhone(req.getPhone());
            d.setAvailable(req.isAvailable()); d.setAvailableDays(req.getAvailableDays());
            d.setExperienceYears(req.getExperienceYears());
            return save(d);
        });
    }

    /**
     * Removes the doctor by id.
     * store.remove() returns the removed value, or null if key didn't exist.
     * We return true/false so the controller can respond with 204 or 404.
     */

    public boolean delete(Long id) { return store.remove(id) != null; }

    /**
     * Private helper — centralizes the write-to-map operation.
     * Returns the doctor so callers can chain: return save(doctor);
     */

    private Doctor save(Doctor d) { store.put(d.getId(), d); return d; }

}
