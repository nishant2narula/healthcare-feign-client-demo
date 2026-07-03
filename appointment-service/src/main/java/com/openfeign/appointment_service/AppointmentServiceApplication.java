package com.openfeign.appointment_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * ================================================================
 * APPOINTMENT SERVICE — Entry Point
 * ================================================================
 * Port: 8081
 * This service is the CORE of the Feign demo.
 *
 * @EnableFeignClients — THE most important annotation here.
 * Without this, Spring does NOT scan for @FeignClient interfaces.
 * With this, Spring:
 * 1. Scans the classpath for interfaces annotated with @FeignClient
 * 2. For each found interface, creates a JDK dynamic proxy at startup
 * 3. Registers that proxy as a Spring bean
 * 4. Allows you to @Autowire / inject the interface anywhere
 * <p>
 * What happens if you forget @EnableFeignClients?
 * You'll get: NoSuchBeanDefinitionException: No qualifying bean of
 * type 'com.healthcare.appointment.client.DoctorClient' found.
 * <p>
 * You can restrict the scan with:
 * @EnableFeignClients(basePackages = "com.healthcare.appointment.client")
 * But the default (scan everything) works fine here.
 * ================================================================
 */

@SpringBootApplication
@EnableFeignClients
public class AppointmentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }

}
