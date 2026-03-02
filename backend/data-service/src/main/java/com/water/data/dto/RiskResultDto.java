package com.water.data.dto;

import com.water.data.model.RiskAssessment;
import com.water.data.model.RiskResult;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for individual community risk results.
 */
@Data
@Builder
public class RiskResultDto {
    private Long id;
    private UUID assessmentId;
    private Long communityId;
    private Integer riskScore;           // 0-100
    private RiskAssessment.RiskLevel riskLevel;

    // Component scores
    private Integer waterQualityScore;
    private Integer accessDistanceScore;
    private Integer infrastructureScore;
    private Integer populationPressureScore;

    private RiskAssessment.ConfidenceLevel confidenceLevel;
    private Integer sampleCount;
    private String explanationJson;      // Top 3 contributing factors
    private LocalDateTime calculatedAt;

    /**
     * Converts RiskResult entity to DTO.
     */
    public static RiskResultDto fromEntity(RiskResult result) {
        return RiskResultDto.builder()
            .id(result.getId())
            .assessmentId(result.getAssessmentId())
            .communityId(result.getCommunityId())
            .riskScore(result.getRiskScore())
            .riskLevel(result.getRiskLevel())
            .waterQualityScore(result.getWaterQualityScore())
            .accessDistanceScore(result.getAccessDistanceScore())
            .infrastructureScore(result.getInfrastructureScore())
            .populationPressureScore(result.getPopulationPressureScore())
            .confidenceLevel(result.getConfidenceLevel())
            .sampleCount(result.getSampleCount())
            .explanationJson(result.getExplanationJson())
            .calculatedAt(result.getCalculatedAt())
            .build();
    }
}
