package com.water.data.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * HydroData entity for hydrological measurements (water quality, levels, flow).
 * Mapped to hydro_data table in PostgreSQL database with PostGIS spatial support.
 */
@Entity
@Table(name = "hydro_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HydroData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_id", columnDefinition = "UUID")
    private UUID uploadId;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    // Source and location
    @Column(nullable = false, length = 100)
    private String source;

    @Column(name = "location_name", length = 200)
    private String locationName;

    @Column(nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point coordinates;

    // Measurement
    @Column(name = "data_type", length = 50)
    private String dataType;

    @Column(name = "parameter_name", length = 100)
    private String parameterName;

    @Column(name = "measurement_value", nullable = false, precision = 15, scale = 4)
    private BigDecimal measurementValue;

    @Column(name = "measurement_unit", nullable = false, length = 50)
    private String measurementUnit;

    @Column(name = "measurement_date", nullable = false)
    private LocalDateTime measurementDate;

    // Additional attributes
    @Column(name = "depth_meters", precision = 10, scale = 2)
    private BigDecimal depthMeters;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(columnDefinition = "JSONB")
    private String metadata;  // Stored as JSON string

    // External source tracking
    @Column(name = "external_source_id", length = 255)
    private String externalSourceId;

    @Column(name = "data_version", length = 50)
    private String dataVersion;

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
}
