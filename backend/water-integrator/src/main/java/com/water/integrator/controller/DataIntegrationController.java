package com.water.integrator.controller;

import com.water.integrator.model.CommunityData;
import com.water.integrator.model.HydroData;
import com.water.integrator.model.InfrastructureData;
import com.water.integrator.service.DataIntegrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST controller for data integration operations.
 * Handles CSV, JSON, and GeoJSON uploads for hydrological,
 * community, and infrastructure data.
 */
@RestController
@RequestMapping("/api/integrator")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DataIntegrationController {

    private final DataIntegrationService integrationService;

    /**
     * Upload hydrological data (CSV, JSON, or GeoJSON)
     */
    @PostMapping("/upload/hydro")
    public ResponseEntity<?> uploadHydroData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) UUID userId) {
        try {
            log.info("Received hydro data upload: {} (size: {} bytes)",
                    file.getOriginalFilename(), file.getSize());

            List<HydroData> data = integrationService.processHydroData(file, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Hydrological data uploaded successfully",
                    "recordsProcessed", data.size()
            ));
        } catch (Exception e) {
            log.error("Error uploading hydro data", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Upload community data (CSV, JSON, or GeoJSON)
     */
    @PostMapping("/upload/community")
    public ResponseEntity<?> uploadCommunityData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) UUID userId) {
        try {
            log.info("Received community data upload: {} (size: {} bytes)",
                    file.getOriginalFilename(), file.getSize());

            List<CommunityData> data = integrationService.processCommunityData(file, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Community data uploaded successfully",
                    "recordsProcessed", data.size()
            ));
        } catch (Exception e) {
            log.error("Error uploading community data", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Upload infrastructure data (CSV, JSON, or GeoJSON)
     */
    @PostMapping("/upload/infrastructure")
    public ResponseEntity<?> uploadInfrastructureData(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) UUID userId) {
        try {
            log.info("Received infrastructure data upload: {} (size: {} bytes)",
                    file.getOriginalFilename(), file.getSize());

            List<InfrastructureData> data = integrationService.processInfrastructureData(file, userId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Infrastructure data uploaded successfully",
                    "recordsProcessed", data.size()
            ));
        } catch (Exception e) {
            log.error("Error uploading infrastructure data", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "error", e.getMessage()));
        }
    }

    /**
     * Get all hydrological data for a user
     */
    @GetMapping("/hydro")
    public ResponseEntity<List<HydroData>> getHydroData(
            @RequestParam(value = "userId", required = false) UUID userId) {
        List<HydroData> data = integrationService.getHydroData(userId);
        return ResponseEntity.ok(data);
    }

    /**
     * Get all community data for a user
     */
    @GetMapping("/community")
    public ResponseEntity<List<CommunityData>> getCommunityData(
            @RequestParam(value = "userId", required = false) UUID userId) {
        List<CommunityData> data = integrationService.getCommunityData(userId);
        return ResponseEntity.ok(data);
    }

    /**
     * Get all infrastructure data for a user
     */
    @GetMapping("/infrastructure")
    public ResponseEntity<List<InfrastructureData>> getInfrastructureData(
            @RequestParam(value = "userId", required = false) UUID userId) {
        List<InfrastructureData> data = integrationService.getInfrastructureData(userId);
        return ResponseEntity.ok(data);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "service", "water-integrator"
        ));
    }
}
