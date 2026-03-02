package com.water.data.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Risk assessment metadata entity.
 * Tracks risk assessment runs and algorithm versions.
 */
@Entity
@Table(name = "risk_assessments")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(columnDefinition = "UUID")
    private UUID id;

    @Column(name = "user_id", nullable = false, columnDefinition = "UUID")
    private UUID userId;

    @Column(nullable = false, length = 255)
    private String name = "Risk Assessment";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "algorithm_version", nullable = false, length = 20)
    private String algorithmVersion = "1.0.0";

    @Column(name = "calculation_duration_ms")
    private Integer calculationDurationMs;

    @Column(name = "total_communities_analyzed")
    private Integer totalCommunitiesAnalyzed;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "is_public", nullable = false)
    private Boolean isPublic = false;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public enum RiskLevel {
        HIGH,    // 67-100
        MEDIUM,  // 34-66
        LOW      // 0-33
    }

    public enum ConfidenceLevel {
        HIGH,    // >30 samples
        MEDIUM,  // 10-30 samples
        LOW,     // 1-9 samples
        NONE     // 0 samples
    }
}
