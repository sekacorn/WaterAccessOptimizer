package com.water.data.dto;

import com.water.data.model.RiskAssessment;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for risk assessment metadata.
 */
@Data
@Builder
public class AssessmentResponseDto {
    private UUID id;
    private UUID userId;
    private String name;
    private String description;
    private String algorithmVersion;
    private Integer calculationDurationMs;
    private Integer totalCommunitiesAnalyzed;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Boolean isPublic;

    /**
     * Converts RiskAssessment entity to DTO.
     */
    public static AssessmentResponseDto fromEntity(RiskAssessment assessment) {
        return AssessmentResponseDto.builder()
            .id(assessment.getId())
            .userId(assessment.getUserId())
            .name(assessment.getName())
            .description(assessment.getDescription())
            .algorithmVersion(assessment.getAlgorithmVersion())
            .calculationDurationMs(assessment.getCalculationDurationMs())
            .totalCommunitiesAnalyzed(assessment.getTotalCommunitiesAnalyzed())
            .createdAt(assessment.getCreatedAt())
            .expiresAt(assessment.getExpiresAt())
            .isPublic(assessment.getIsPublic())
            .build();
    }
}
