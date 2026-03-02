package com.water.data.controller;

import com.water.data.dto.AssessmentResponseDto;
import com.water.data.dto.AssessmentSummaryDto;
import com.water.data.dto.CreateAssessmentRequest;
import com.water.data.dto.RiskResultDto;
import com.water.data.model.RiskAssessment;
import com.water.data.model.RiskResult;
import com.water.data.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

/**
 * REST controller for risk assessment operations.
 * Provides endpoints for creating, listing, and analyzing water access risk assessments.
 */
@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
@Slf4j
@Tag(
    name = "Risk Assessment",
    description = "Multi-criteria water access risk assessment API. Calculate risk scores for communities based on water quality, access distance, infrastructure, and population pressure."
)
@SecurityRequirement(name = "bearerAuth")
public class RiskAssessmentController {

    private final RiskAssessmentService assessmentService;

    /**
     * POST /api/v1/risk/assessments
     * Creates a new risk assessment by calculating scores for all communities.
     *
     * @param request Assessment metadata
     * @param authentication JWT authentication
     * @return Created assessment with metadata
     */
    @Operation(
        summary = "Create new risk assessment",
        description = """
            Calculates water access risk scores for all communities using multi-criteria analysis:
            - Water Quality (35% weight): WHO guideline violations
            - Access Distance (30% weight): Proximity to water sources
            - Infrastructure (25% weight): Facility operational status
            - Population Pressure (10% weight): People per facility ratio

            Risk Levels: HIGH (67-100), MEDIUM (34-66), LOW (0-33)
            Confidence Levels: HIGH (>30 samples), MEDIUM (10-30), LOW (1-9), NONE (0)

            Typical execution time: 2-5 seconds for 500 communities.
            """,
        tags = {"Risk Assessment"}
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "201",
            description = "Assessment created successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AssessmentResponseDto.class)
            )
        ),
        @ApiResponse(
            responseCode = "401",
            description = "Unauthorized - Invalid or missing JWT token",
            content = @Content
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during risk calculation",
            content = @Content
        )
    })
    @PostMapping("/assessments")
    public ResponseEntity<AssessmentResponseDto> createAssessment(
        @Parameter(
            description = "Assessment metadata (name, description, isPublic)",
            required = true
        )
        @RequestBody CreateAssessmentRequest request,
        @Parameter(hidden = true) Authentication authentication
    ) {
        String userId = authentication.getName();

        log.info("User {} creating risk assessment: {}", userId, request.getName());

        try {
            RiskAssessment assessment = assessmentService.createAssessment(
                userId,
                request.getName(),
                request.getDescription(),
                request.getIsPublic()
            );

            AssessmentResponseDto response = AssessmentResponseDto.fromEntity(assessment);

            log.info("Created assessment {} with {} communities in {}ms",
                assessment.getId(),
                assessment.getTotalCommunitiesAnalyzed(),
                assessment.getCalculationDurationMs()
            );

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (Exception e) {
            log.error("Failed to create assessment for user {}: {}", userId, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/v1/risk/assessments
     * Lists all assessments created by the authenticated user.
     *
     * @param authentication JWT authentication
     * @return List of user's assessments (most recent first)
     */
    @GetMapping("/assessments")
    public ResponseEntity<List<AssessmentResponseDto>> listUserAssessments(
        Authentication authentication
    ) {
        String userId = authentication.getName();

        List<RiskAssessment> assessments = assessmentService.listUserAssessments(userId);

        List<AssessmentResponseDto> response = assessments.stream()
            .map(AssessmentResponseDto::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/risk/assessments/public
     * Lists all public assessments (visible to all users).
     *
     * @return List of public assessments
     */
    @GetMapping("/assessments/public")
    public ResponseEntity<List<AssessmentResponseDto>> listPublicAssessments() {
        List<RiskAssessment> assessments = assessmentService.listPublicAssessments();

        List<AssessmentResponseDto> response = assessments.stream()
            .map(AssessmentResponseDto::fromEntity)
            .collect(Collectors.toList());

        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/v1/risk/assessments/{id}
     * Gets details for a specific assessment.
     * User must be owner or assessment must be public.
     *
     * @param id Assessment ID
     * @param authentication JWT authentication
     * @return Assessment details
     */
    @GetMapping("/assessments/{id}")
    public ResponseEntity<AssessmentResponseDto> getAssessment(
        @PathVariable String id,
        Authentication authentication
    ) {
        String userId = authentication.getName();

        try {
            RiskAssessment assessment = assessmentService.getAssessment(id, userId);
            return ResponseEntity.ok(AssessmentResponseDto.fromEntity(assessment));
        } catch (RuntimeException e) {
            log.error("Error getting assessment {}: {}", id, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/v1/risk/assessments/{id}/results
     * Gets all risk results for an assessment (ordered by risk score, highest first).
     * Optional filter by risk level.
     *
     * @param id Assessment ID
     * @param riskLevel Optional filter (HIGH, MEDIUM, LOW)
     * @param authentication JWT authentication
     * @return List of risk results
     */
    @GetMapping("/assessments/{id}/results")
    public ResponseEntity<List<RiskResultDto>> getAssessmentResults(
        @PathVariable String id,
        @RequestParam(required = false) RiskAssessment.RiskLevel riskLevel,
        Authentication authentication
    ) {
        String userId = authentication.getName();

        try {
            List<RiskResult> results;

            if (riskLevel != null) {
                results = assessmentService.getResultsByRiskLevel(id, userId, riskLevel);
            } else {
                results = assessmentService.getAssessmentResults(id, userId);
            }

            List<RiskResultDto> response = results.stream()
                .map(RiskResultDto::fromEntity)
                .collect(Collectors.toList());

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error getting results for assessment {}: {}", id, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * GET /api/v1/risk/assessments/{id}/summary
     * Gets summary statistics for an assessment.
     *
     * @param id Assessment ID
     * @param authentication JWT authentication
     * @return Summary statistics (counts, percentages, duration)
     */
    @GetMapping("/assessments/{id}/summary")
    public ResponseEntity<AssessmentSummaryDto> getAssessmentSummary(
        @PathVariable String id,
        Authentication authentication
    ) {
        String userId = authentication.getName();

        try {
            RiskAssessmentService.AssessmentSummary summary =
                assessmentService.getAssessmentSummary(id, userId);

            AssessmentSummaryDto response = AssessmentSummaryDto.fromServiceSummary(summary);

            return ResponseEntity.ok(response);

        } catch (RuntimeException e) {
            log.error("Error getting summary for assessment {}: {}", id, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * DELETE /api/v1/risk/assessments/{id}
     * Deletes an assessment and all its results.
     * Only the owner can delete.
     *
     * @param id Assessment ID
     * @param authentication JWT authentication
     * @return Success response
     */
    @DeleteMapping("/assessments/{id}")
    public ResponseEntity<?> deleteAssessment(
        @PathVariable String id,
        Authentication authentication
    ) {
        String userId = authentication.getName();

        try {
            assessmentService.deleteAssessment(id, userId);

            log.info("User {} deleted assessment {}", userId, id);

            return ResponseEntity.ok(new DeleteResponse("success", "Assessment deleted successfully"));

        } catch (RuntimeException e) {
            log.error("Error deleting assessment {}: {}", id, e.getMessage());
            if (e.getMessage().contains("not found")) {
                return ResponseEntity.notFound().build();
            } else if (e.getMessage().contains("Unauthorized")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new DeleteResponse("error", "Only the owner can delete this assessment"));
            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new DeleteResponse("error", "Failed to delete assessment"));
        }
    }

    /**
     * Simple response for delete operations.
     */
    private record DeleteResponse(String status, String message) {}
}
