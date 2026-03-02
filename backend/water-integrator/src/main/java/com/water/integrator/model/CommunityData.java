package com.water.integrator.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Entity representing community data including population,
 * water access levels, and needs assessment.
 */
@Entity
@Table(name = "community_data")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommunityData {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "community_name")
    private String communityName;

    @Column(columnDefinition = "geography(Point, 4326)")
    private Point location;

    private Integer population;

    @Column(name = "water_access_level", length = 50)
    private String waterAccessLevel; // no_access, basic, limited, safely_managed

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "access_points", columnDefinition = "jsonb")
    private Map<String, Object> accessPoints;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "needs_assessment", columnDefinition = "jsonb")
    private Map<String, Object> needsAssessment;

    @Column(length = 100)
    private String source; // OpenStreetMap, local survey, etc.

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
