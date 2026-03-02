package com.water.data.service;

import com.water.data.model.CommunityData;
import com.water.data.model.RiskAssessment;
import com.water.data.model.RiskResult;
import com.water.data.repository.CommunityDataRepository;
import com.water.data.repository.RiskAssessmentRepository;
import com.water.data.repository.RiskResultRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing risk assessments and their results.
 * Orchestrates risk scoring across all communities.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentService {

    private final RiskAssessmentRepository assessmentRepository;
    private final RiskResultRepository resultRepository;
    private final CommunityDataRepository communityDataRepository;
    private final RiskScoringService riskScoringService;

    private static final String ALGORITHM_VERSION = "1.0.0";

    /**
     * Creates a new risk assessment by calculating scores for all communities.
     *
     * @param userId User ID creating the assessment
     * @param name Assessment name (optional)
     * @param description Assessment description (optional)
     * @param isPublic Whether assessment is public
     * @return The created risk assessment with ID
     */
    @Transactional
    public RiskAssessment createAssessment(String userId, String name, String description, Boolean isPublic) {
        long startTime = System.currentTimeMillis();

        // 1. Create assessment metadata
        RiskAssessment assessment = new RiskAssessment();
        assessment.setUserId(UUID.fromString(userId));
        assessment.setName(name != null ? name : "Risk Assessment");
        assessment.setDescription(description);
        assessment.setAlgorithmVersion(ALGORITHM_VERSION);
        assessment.setIsPublic(isPublic != null ? isPublic : false);
        assessment.setCreatedAt(LocalDateTime.now());

        // Set expiration to 90 days from now
        assessment.setExpiresAt(LocalDateTime.now().plusDays(90));

        // Save assessment to get ID
        assessment = assessmentRepository.save(assessment);

        log.info("Created risk assessment {} for user {}", assessment.getId(), userId);

        // 2. Get all communities (not deleted)
        List<CommunityData> communities = communityDataRepository.findAll();
        List<RiskResult> results = new ArrayList<>();

        log.info("Calculating risk scores for {} communities", communities.size());

        // 3. Calculate risk score for each community
        for (CommunityData community : communities) {
            try {
                RiskScoringService.RiskScoreResult scoreResult =
                    riskScoringService.calculateRiskScore(community);

                // Create RiskResult entity
                RiskResult result = new RiskResult();
                result.setAssessmentId(assessment.getId());
                result.setCommunityId(scoreResult.getCommunityId());
                result.setRiskScore(scoreResult.getOverallScore());
                result.setRiskLevel(scoreResult.getRiskLevel());
                result.setWaterQualityScore(scoreResult.getWaterQualityScore());
                result.setAccessDistanceScore(scoreResult.getAccessDistanceScore());
                result.setInfrastructureScore(scoreResult.getInfrastructureScore());
                result.setPopulationPressureScore(scoreResult.getPopulationPressureScore());
                result.setConfidenceLevel(scoreResult.getConfidenceLevel());
                result.setSampleCount(scoreResult.getSampleCount());
                result.setExplanationJson(scoreResult.getExplanationJson());
                result.setCalculatedAt(LocalDateTime.now());

                results.add(result);
            } catch (Exception e) {
                log.error("Failed to calculate risk score for community {}: {}",
                    community.getId(), e.getMessage());
                // Continue with other communities
            }
        }

        // 4. Bulk save all results
        if (!results.isEmpty()) {
            resultRepository.saveAll(results);
            log.info("Saved {} risk results", results.size());
        }

        // 5. Update assessment metadata
        long endTime = System.currentTimeMillis();
        assessment.setCalculationDurationMs((int) (endTime - startTime));
        assessment.setTotalCommunitiesAnalyzed(results.size());
        assessment = assessmentRepository.save(assessment);

        log.info("Risk assessment {} completed in {}ms",
            assessment.getId(), assessment.getCalculationDurationMs());

        return assessment;
    }

    /**
     * Lists all assessments for a user (most recent first).
     */
    public List<RiskAssessment> listUserAssessments(String userId) {
        return assessmentRepository.findByUserIdOrderByCreatedAtDesc(UUID.fromString(userId));
    }

    /**
     * Lists all public assessments.
     */
    public List<RiskAssessment> listPublicAssessments() {
        return assessmentRepository.findByIsPublicTrue();
    }

    /**
     * Gets a specific assessment by ID.
     * Validates user access (must be owner or public).
     */
    public RiskAssessment getAssessment(String assessmentId, String userId) {
        UUID assessmentUUID = UUID.fromString(assessmentId);
        UUID userUUID = UUID.fromString(userId);

        RiskAssessment assessment = assessmentRepository.findById(assessmentUUID)
            .orElseThrow(() -> new RuntimeException("Assessment not found: " + assessmentId));

        // Check access: must be owner or public
        if (!assessment.getUserId().equals(userUUID) && !assessment.getIsPublic()) {
            throw new RuntimeException("Unauthorized access to assessment: " + assessmentId);
        }

        return assessment;
    }

    /**
     * Gets all risk results for an assessment (ordered by risk score descending).
     * Validates user access.
     */
    public List<RiskResult> getAssessmentResults(String assessmentId, String userId) {
        // First validate access to assessment
        getAssessment(assessmentId, userId);

        // Return results ordered by risk score (highest first)
        return resultRepository.findByAssessmentIdOrderByRiskScoreDesc(UUID.fromString(assessmentId));
    }

    /**
     * Gets risk results filtered by risk level.
     * Validates user access.
     */
    public List<RiskResult> getResultsByRiskLevel(String assessmentId, String userId,
                                                   RiskAssessment.RiskLevel riskLevel) {
        // Validate access
        getAssessment(assessmentId, userId);

        // Get all results and filter by risk level
        List<RiskResult> allResults = resultRepository.findByAssessmentIdOrderByRiskScoreDesc(
            UUID.fromString(assessmentId)
        );

        return allResults.stream()
            .filter(r -> r.getRiskLevel() == riskLevel)
            .toList();
    }

    /**
     * Gets summary statistics for an assessment.
     */
    public AssessmentSummary getAssessmentSummary(String assessmentId, String userId) {
        // Validate access
        RiskAssessment assessment = getAssessment(assessmentId, userId);

        UUID assessmentUUID = UUID.fromString(assessmentId);

        // Count by risk level
        Long highCount = resultRepository.countByAssessmentIdAndRiskLevel(
            assessmentUUID, RiskAssessment.RiskLevel.HIGH
        );
        Long mediumCount = resultRepository.countByAssessmentIdAndRiskLevel(
            assessmentUUID, RiskAssessment.RiskLevel.MEDIUM
        );
        Long lowCount = resultRepository.countByAssessmentIdAndRiskLevel(
            assessmentUUID, RiskAssessment.RiskLevel.LOW
        );

        return AssessmentSummary.builder()
            .assessmentId(assessmentUUID)
            .totalCommunities(assessment.getTotalCommunitiesAnalyzed())
            .highRiskCount(highCount.intValue())
            .mediumRiskCount(mediumCount.intValue())
            .lowRiskCount(lowCount.intValue())
            .calculationDurationMs(assessment.getCalculationDurationMs())
            .createdAt(assessment.getCreatedAt())
            .build();
    }

    /**
     * Deletes an assessment and all its results.
     * Only the owner can delete.
     */
    @Transactional
    public void deleteAssessment(String assessmentId, String userId) {
        UUID assessmentUUID = UUID.fromString(assessmentId);
        UUID userUUID = UUID.fromString(userId);

        RiskAssessment assessment = assessmentRepository.findById(assessmentUUID)
            .orElseThrow(() -> new RuntimeException("Assessment not found: " + assessmentId));

        // Verify ownership (cannot delete others' assessments, even if public)
        if (!assessment.getUserId().equals(userUUID)) {
            throw new RuntimeException("Unauthorized: Only the owner can delete this assessment");
        }

        // Delete all results first (cascade)
        List<RiskResult> results = resultRepository.findByAssessmentIdOrderByRiskScoreDesc(assessmentUUID);
        resultRepository.deleteAll(results);

        log.info("Deleted {} results for assessment {}", results.size(), assessmentId);

        // Delete assessment
        assessmentRepository.delete(assessment);

        log.info("Deleted assessment {} by user {}", assessmentId, userId);
    }

    /**
     * Summary statistics for a risk assessment.
     */
    @lombok.Builder
    @lombok.Data
    public static class AssessmentSummary {
        private UUID assessmentId;
        private Integer totalCommunities;
        private Integer highRiskCount;
        private Integer mediumRiskCount;
        private Integer lowRiskCount;
        private Integer calculationDurationMs;
        private LocalDateTime createdAt;
    }
}
