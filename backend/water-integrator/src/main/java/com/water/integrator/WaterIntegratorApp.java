package com.water.integrator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * WaterAccessOptimizer - Water Integrator Service
 *
 * This microservice integrates hydrological data (USGS, WHO),
 * community data (OpenStreetMap), and infrastructure data from
 * various sources. Supports CSV, JSON, and GeoJSON formats.
 *
 * @author WaterAccessOptimizer Team
 * @version 1.0.0
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
public class WaterIntegratorApp {

    public static void main(String[] args) {
        SpringApplication.run(WaterIntegratorApp.class, args);
        System.out.println("=================================================");
        System.out.println("  Water Integrator Service Started Successfully");
        System.out.println("  Port: 8081");
        System.out.println("  Environment: " + System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "default"));
        System.out.println("=================================================");
    }
}
