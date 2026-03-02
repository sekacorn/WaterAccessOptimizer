package com.water.data.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * InfrastructureData entity for water facilities and infrastructure.
 * Mapped to infrastructure_data table in PostgreSQL database with PostGIS spatial support.
 */
@Entity
@Table(name = "infrastructure_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfrastructureData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_id", columnDefinition = "UUID")
    private UUID uploadId;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    // Facility info
    @Column(name = "facility_type", nullable = false, length = 100)
    @Enumerated(EnumType.STRING)
    private FacilityType facilityType;

    @Column(name = "facility_name", nullable = false, length = 200)
    private String facilityName;

    @Column(nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point coordinates;

    // Operational status
    @Column(name = "operational_status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private OperationalStatus operationalStatus;

    // Capacity
    @Column(precision = 15, scale = 2)
    private BigDecimal capacity;

    @Column(name = "capacity_unit", length = 50)
    private String capacityUnit;

    @Column(name = "population_served")
    private Integer populationServed;

    // Dates
    @Column(name = "installation_date")
    private LocalDate installationDate;

    @Column(name = "last_maintenance_date")
    private LocalDate lastMaintenanceDate;

    // Additional attributes
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "JSONB")
    private String metadata;  // Stored as JSON string

    // External source tracking
    @Column(name = "external_source_id", length = 255)
    private String externalSourceId;

    @Column(nullable = false, length = 100)
    private String source;

    // Provenance
    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "last_updated_at", nullable = false)
    private LocalDateTime lastUpdatedAt;

    @PrePersist
    protected void onCreate() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
        lastUpdatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        lastUpdatedAt = LocalDateTime.now();
    }

    /**
     * Facility type enum.
     */
    public enum FacilityType {
        WELL,
        BOREHOLE,
        TREATMENT_PLANT,
        RESERVOIR,
        DISTRIBUTION_POINT,
        PUMP_STATION,
        WATER_TOWER,
        SPRING_PROTECTION,
        OTHER
    }

    /**
     * Operational status enum.
     */
    public enum OperationalStatus {
        OPERATIONAL,
        NON_OPERATIONAL,
        UNDER_MAINTENANCE,
        PLANNED,
        ABANDONED
    }

    /**
     * Gets latitude from coordinates.
     *
     * @return Latitude in decimal degrees
     */
    public double getLatitude() {
        return coordinates != null ? coordinates.getY() : 0.0;
    }

    /**
     * Gets longitude from coordinates.
     *
     * @return Longitude in decimal degrees
     */
    public double getLongitude() {
        return coordinates != null ? coordinates.getX() : 0.0;
    }

    /**
     * Checks if facility is operational.
     *
     * @return true if facility is operational
     */
    public boolean isOperational() {
        return operationalStatus == OperationalStatus.OPERATIONAL;
    }
}
