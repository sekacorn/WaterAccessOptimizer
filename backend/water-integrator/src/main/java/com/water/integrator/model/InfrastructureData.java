package com.water.integrator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing water infrastructure data including
 * treatment plants, pipelines, pump stations, and reservoirs.
 */
@Entity
@Table(name = "infrastructure_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfrastructureData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "facility_type", length = 100)
    private String facilityType; // treatment_plant, pipeline, pump_station, reservoir

    @Column(name = "facility_name")
    private String facilityName;

    @Column(columnDefinition = "geography(Point, 4326)")
    private Point location;

    @Column(precision = 12, scale = 2)
    private BigDecimal capacity;

    @Column(name = "capacity_unit", length = 50)
    private String capacityUnit;

    @Column(name = "operational_status", length = 50)
    private String operationalStatus; // operational, maintenance, non_operational

    @Column(name = "service_area", columnDefinition = "geography(Polygon, 4326)")
    private Polygon serviceArea;

    @Column(name = "construction_date")
    private LocalDate constructionDate;

    @Column(name = "last_maintenance")
    private LocalDateTime lastMaintenance;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
