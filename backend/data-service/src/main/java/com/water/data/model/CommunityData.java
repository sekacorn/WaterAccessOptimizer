package com.water.data.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Point;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * CommunityData entity for population and water access data.
 * Mapped to community_data table in PostgreSQL database with PostGIS spatial support.
 */
@Entity
@Table(name = "community_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "upload_id", columnDefinition = "UUID")
    private UUID uploadId;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    // Community info
    @Column(name = "community_name", nullable = false, length = 200)
    private String communityName;

    @Column(nullable = false, columnDefinition = "geography(Point, 4326)")
    private Point coordinates;

    @Column(nullable = false)
    private Integer population;

    @Column(name = "household_count")
    private Integer householdCount;

    // Water access
    @Column(name = "water_access_level", length = 50)
    @Enumerated(EnumType.STRING)
    private WaterAccessLevel waterAccessLevel;

    @Column(name = "primary_water_source", length = 100)
    private String primaryWaterSource;

    @Column(name = "collection_date")
    private LocalDate collectionDate;

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
     * Water access level enum based on WHO JMP Service Ladder.
     */
    public enum WaterAccessLevel {
        NONE,           // No access to improved water source
        LIMITED,        // Improved source, >30 minutes collection time
        BASIC,          // Improved source, ≤30 minutes collection time
        SAFELY_MANAGED  // Improved source, on premises, available when needed
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
