package com.water.data.validator;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 5 Validation: Quality Assessment
 * Calculates completeness score and confidence level for uploaded data.
 */
@Component
public class QualityAssessment {

    private final SchemaValidator schemaValidator;

    public QualityAssessment(SchemaValidator schemaValidator) {
        this.schemaValidator = schemaValidator;
    }

    /**
     * Assesses quality of uploaded dataset.
     *
     * @param rows Validated rows
     * @param dataType Type of data
     * @param errorCount Number of validation errors
     * @param warningCount Number of validation warnings
     * @return Quality assessment result
     */
    public QualityAssessmentResult assess(
        List<Map<String, String>> rows,
        SchemaValidator.DataType dataType,
        long errorCount,
        long warningCount
    ) {
        // Calculate completeness score
        int completenessScore = calculateCompletenessScore(rows, dataType);

        // Calculate confidence level
        ConfidenceLevel confidenceLevel = calculateConfidenceLevel(
            rows.size(),
            completenessScore,
            errorCount,
            warningCount
        );

        return new QualityAssessmentResult(
            completenessScore,
            confidenceLevel,
            errorCount,
            warningCount
        );
    }

    /**
     * Calculates completeness score (0-100) based on optional field population.
     */
    private int calculateCompletenessScore(
        List<Map<String, String>> rows,
        SchemaValidator.DataType dataType
    ) {
        if (rows.isEmpty()) {
            return 0;
        }

        Set<String> optionalFields = schemaValidator.getOptionalColumns(dataType);
        if (optionalFields.isEmpty()) {
            return 100; // No optional fields
        }

        int totalOptionalFields = optionalFields.size() * rows.size();
        int populatedOptionalFields = 0;

        for (Map<String, String> row : rows) {
            for (String field : optionalFields) {
                String value = row.get(field);
                if (value != null && !value.trim().isEmpty()) {
                    populatedOptionalFields++;
                }
            }
        }

        return (populatedOptionalFields * 100) / totalOptionalFields;
    }

    /**
     * Calculates confidence level based on data quantity and quality.
     *
     * Scoring algorithm (from Agent 02 specification):
     * - Completeness: 0-40 points (>75% = 40, >50% = 25, else 10)
     * - Error count: -10 points per error (min 0)
     * - Warning count: -2 points per warning (min 0)
     * - Row count: 0-60 points (>30 = 60, >10 = 40, >5 = 20, else 10)
     *
     * Thresholds:
     * - HIGH: ≥80 points
     * - MEDIUM: 50-79 points
     * - LOW: 20-49 points
     * - NONE: <20 points
     */
    private ConfidenceLevel calculateConfidenceLevel(
        int rowCount,
        int completenessScore,
        long errorCount,
        long warningCount
    ) {
        int score = 0;

        // Completeness points (0-40)
        if (completenessScore > 75) {
            score += 40;
        } else if (completenessScore > 50) {
            score += 25;
        } else {
            score += 10;
        }

        // Error penalty (-10 points per error, min 0)
        score -= (int) (errorCount * 10);
        score = Math.max(0, score);

        // Warning penalty (-2 points per warning)
        score -= (int) (warningCount * 2);
        score = Math.max(0, score);

        // Row count points (0-60)
        if (rowCount >= 30) {
            score += 60;
        } else if (rowCount >= 10) {
            score += 40;
        } else if (rowCount >= 5) {
            score += 20;
        } else if (rowCount >= 1) {
            score += 10;
        }

        // Convert score to confidence level
        if (score >= 80) {
            return ConfidenceLevel.HIGH;
        } else if (score >= 50) {
            return ConfidenceLevel.MEDIUM;
        } else if (score >= 20) {
            return ConfidenceLevel.LOW;
        } else {
            return ConfidenceLevel.NONE;
        }
    }

    /**
     * Result of quality assessment.
     */
    public static class QualityAssessmentResult {
        private final int completenessScore;      // 0-100
        private final ConfidenceLevel confidenceLevel;
        private final long errorCount;
        private final long warningCount;

        public QualityAssessmentResult(
            int completenessScore,
            ConfidenceLevel confidenceLevel,
            long errorCount,
            long warningCount
        ) {
            this.completenessScore = completenessScore;
            this.confidenceLevel = confidenceLevel;
            this.errorCount = errorCount;
            this.warningCount = warningCount;
        }

        public int getCompletenessScore() {
            return completenessScore;
        }

        public ConfidenceLevel getConfidenceLevel() {
            return confidenceLevel;
        }

        public long getErrorCount() {
            return errorCount;
        }

        public long getWarningCount() {
            return warningCount;
        }

        /**
         * Returns human-readable quality summary.
         */
        public String getSummary() {
            return String.format(
                "Completeness: %d%%, Confidence: %s, Errors: %d, Warnings: %d",
                completenessScore,
                confidenceLevel,
                errorCount,
                warningCount
            );
        }
    }

    /**
     * Confidence levels for data quality.
     */
    public enum ConfidenceLevel {
        HIGH,      // ≥80 points: >30 rows, >75% completeness, no/few errors
        MEDIUM,    // 50-79 points: >10 rows, >50% completeness, some errors
        LOW,       // 20-49 points: >5 rows, basic completeness, many errors
        NONE       // <20 points: <5 rows or critical quality issues
    }
}
