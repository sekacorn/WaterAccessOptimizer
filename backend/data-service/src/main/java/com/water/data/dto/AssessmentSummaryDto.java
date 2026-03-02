package com.water.data.dto;

import com.water.data.service.RiskAssessmentService;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for risk assessment summary statistics.
 */
@Data
@Builder
public class AssessmentSummaryDto {
    private UUID assessmentId;
    private Integer totalCommunities;
    private Integer highRiskCount;
    private Integer mediumRiskCount;
    private Integer lowRiskCount;
    private Integer calculationDurationMs;
    private LocalDateTime createdAt;

    // Calculated percentages
    private Double highRiskPercentage;
    private Double mediumRiskPercentage;
    private Double lowRiskPercentage;

    /**
     * Converts service summary to DTO with calculated percentages.
     */
    public static AssessmentSummaryDto fromServiceSummary(RiskAssessmentService.AssessmentSummary summary) {
        int total = summary.getTotalCommunities();

        double highPct = total > 0 ? (summary.getHighRiskCount() * 100.0 / total) : 0.0;
        double mediumPct = total > 0 ? (summary.getMediumRiskCount() * 100.0 / total) : 0.0;
        double lowPct = total > 0 ? (summary.getLowRiskCount() * 100.0 / total) : 0.0;

        return AssessmentSummaryDto.builder()
            .assessmentId(summary.getAssessmentId())
            .totalCommunities(summary.getTotalCommunities())
            .highRiskCount(summary.getHighRiskCount())
            .mediumRiskCount(summary.getMediumRiskCount())
            .lowRiskCount(summary.getLowRiskCount())
            .calculationDurationMs(summary.getCalculationDurationMs())
            .createdAt(summary.getCreatedAt())
            .highRiskPercentage(Math.round(highPct * 100.0) / 100.0)  // Round to 2 decimals
            .mediumRiskPercentage(Math.round(mediumPct * 100.0) / 100.0)
            .lowRiskPercentage(Math.round(lowPct * 100.0) / 100.0)
            .build();
    }
}
