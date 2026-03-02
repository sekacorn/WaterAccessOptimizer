package com.water.integrator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing hydrological data from sources like USGS, WHO.
 * Includes water quality, aquifer levels, rainfall, etc.
 */
@Entity
@Table(name = "hydro_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class HydroData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(length = 100)
    private String source; // USGS, WHO, etc.

    @Column(name = "data_type", length = 50)
    private String dataType; // water_quality, aquifer_level, rainfall, etc.

    @Column(columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "measurement_value", precision = 10, scale = 4)
    private BigDecimal measurementValue;

    @Column(name = "measurement_unit", length = 50)
    private String measurementUnit;

    @Column(name = "measurement_date")
    private LocalDateTime measurementDate;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
