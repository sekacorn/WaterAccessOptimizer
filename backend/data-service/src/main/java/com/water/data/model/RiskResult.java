package com.water.data.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Risk result entity for individual community risk scores.
 */
@Entity
@Table(name = "risk_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assessment_id", nullable = false, columnDefinition = "UUID")
    private UUID assessmentId;

    @Column(name = "community_id", nullable = false)
    private Long communityId;

    @Column(name = "risk_score", nullable = false)
    private Integer riskScore;  // 0-100

    @Column(name = "risk_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RiskAssessment.RiskLevel riskLevel;

    // Component scores (weighted sum = riskScore)
    @Column(name = "water_quality_score")
    private Integer waterQualityScore;  // 35% weight

    @Column(name = "access_distance_score")
    private Integer accessDistanceScore;  // 30% weight

    @Column(name = "infrastructure_score")
    private Integer infrastructureScore;  // 25% weight

    @Column(name = "population_pressure_score")
    private Integer populationPressureScore;  // 10% weight

    @Column(name = "confidence_level", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private RiskAssessment.ConfidenceLevel confidenceLevel;

    @Column(name = "sample_count", nullable = false)
    private Integer sampleCount;

    @Column(name = "explanation_json", nullable = false, columnDefinition = "JSONB")
    private String explanationJson;  // Top 3 contributing factors

    @Column(name = "calculated_at", nullable = false)
    private LocalDateTime calculatedAt;

    @PrePersist
    protected void onCreate() {
        if (calculatedAt == null) {
            calculatedAt = LocalDateTime.now();
        }
    }
}
