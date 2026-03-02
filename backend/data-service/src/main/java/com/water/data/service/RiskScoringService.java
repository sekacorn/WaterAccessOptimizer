package com.water.data.service;

import com.water.data.model.CommunityData;
import com.water.data.model.HydroData;
import com.water.data.model.InfrastructureData;
import com.water.data.model.RiskAssessment;
import com.water.data.repository.CommunityDataRepository;
import com.water.data.repository.HydroDataRepository;
import com.water.data.repository.InfrastructureDataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Core risk scoring service implementing multi-criteria analysis.
 *
 * Risk Algorithm v1.0.0:
 * - Water Quality Score: 35% weight
 * - Access Distance Score: 30% weight
 * - Infrastructure Score: 25% weight
 * - Population Pressure Score: 10% weight
 *
 * Risk Levels:
 * - HIGH: 67-100
 * - MEDIUM: 34-66
 * - LOW: 0-33
 *
 * Confidence Levels (based on sample count):
 * - HIGH: >30 samples
 * - MEDIUM: 10-30 samples
 * - LOW: 1-9 samples
 * - NONE: 0 samples
 */
@Service
@RequiredArgsConstructor
public class RiskScoringService {

    private final HydroDataRepository hydroDataRepository;
    private final CommunityDataRepository communityDataRepository;
    private final InfrastructureDataRepository infrastructureDataRepository;

    // Weights for multi-criteria analysis (must sum to 1.0)
    private static final double WATER_QUALITY_WEIGHT = 0.35;
    private static final double ACCESS_DISTANCE_WEIGHT = 0.30;
    private static final double INFRASTRUCTURE_WEIGHT = 0.25;
    private static final double POPULATION_PRESSURE_WEIGHT = 0.10;

    // Distance thresholds (in meters)
    private static final double SAFE_DISTANCE = 500;       // <500m = safe
    private static final double MODERATE_DISTANCE = 1500;  // 500-1500m = moderate
    private static final double CRITICAL_DISTANCE = 5000;  // >5000m = critical

    // WHO Water Quality Guidelines (mg/L)
    private static final double WHO_ARSENIC_LIMIT = 0.01;
    private static final double WHO_FLUORIDE_LIMIT = 1.5;
    private static final double WHO_NITRATE_LIMIT = 50.0;
    private static final double WHO_ECOLI_LIMIT = 0.0;  // Should be 0

    /**
     * Calculates risk score for a single community.
     */
    public RiskScoreResult calculateRiskScore(CommunityData community) {
        // 1. Calculate component scores
        int waterQualityScore = calculateWaterQualityScore(community);
        int accessDistanceScore = calculateAccessDistanceScore(community);
        int infrastructureScore = calculateInfrastructureScore(community);
        int populationPressureScore = calculatePopulationPressureScore(community);

        // 2. Calculate weighted overall score
        double weightedScore =
            (waterQualityScore * WATER_QUALITY_WEIGHT) +
            (accessDistanceScore * ACCESS_DISTANCE_WEIGHT) +
            (infrastructureScore * INFRASTRUCTURE_WEIGHT) +
            (populationPressureScore * POPULATION_PRESSURE_WEIGHT);

        int overallScore = (int) Math.round(weightedScore);

        // 3. Determine risk level
        RiskAssessment.RiskLevel riskLevel = determineRiskLevel(overallScore);

        // 4. Calculate confidence level
        int sampleCount = countNearbyMeasurements(community);
        RiskAssessment.ConfidenceLevel confidenceLevel = determineConfidenceLevel(sampleCount);

        // 5. Generate explanation (top 3 contributing factors)
        String explanationJson = generateExplanation(
            waterQualityScore, accessDistanceScore,
            infrastructureScore, populationPressureScore
        );

        return RiskScoreResult.builder()
            .communityId(community.getId())
            .overallScore(overallScore)
            .riskLevel(riskLevel)
            .waterQualityScore(waterQualityScore)
            .accessDistanceScore(accessDistanceScore)
            .infrastructureScore(infrastructureScore)
            .populationPressureScore(populationPressureScore)
            .confidenceLevel(confidenceLevel)
            .sampleCount(sampleCount)
            .explanationJson(explanationJson)
            .build();
    }

    /**
     * Water Quality Score (35% weight)
     * Analyzes contamination levels vs WHO guidelines.
     */
    private int calculateWaterQualityScore(CommunityData community) {
        // Find all water quality measurements within 5km radius
        List<HydroData> measurements = hydroDataRepository.findWithinRadius(
            community.getCoordinates().getX(),  // longitude
            community.getCoordinates().getY(),  // latitude
            5000.0  // 5km radius
        );

        if (measurements.isEmpty()) {
            return 50;  // Default moderate risk if no data
        }

        int totalRisk = 0;
        int validMeasurements = 0;

        for (HydroData hydro : measurements) {
            int measurementRisk = 0;

            // Arsenic check
            if (hydro.getArsenicMgL() != null) {
                double arsenicRatio = hydro.getArsenicMgL() / WHO_ARSENIC_LIMIT;
                if (arsenicRatio > 2.0) measurementRisk += 25;
                else if (arsenicRatio > 1.0) measurementRisk += 15;
                else if (arsenicRatio > 0.5) measurementRisk += 5;
            }

            // Fluoride check
            if (hydro.getFluorideMgL() != null) {
                double fluorideRatio = hydro.getFluorideMgL() / WHO_FLUORIDE_LIMIT;
                if (fluorideRatio > 2.0) measurementRisk += 25;
                else if (fluorideRatio > 1.0) measurementRisk += 15;
                else if (fluorideRatio > 0.5) measurementRisk += 5;
            }

            // Nitrate check
            if (hydro.getNitrateMgL() != null) {
                double nitrateRatio = hydro.getNitrateMgL() / WHO_NITRATE_LIMIT;
                if (nitrateRatio > 2.0) measurementRisk += 25;
                else if (nitrateRatio > 1.0) measurementRisk += 15;
                else if (nitrateRatio > 0.5) measurementRisk += 5;
            }

            // E. coli check
            if (hydro.getEcoliCfu100ml() != null && hydro.getEcoliCfu100ml() > WHO_ECOLI_LIMIT) {
                if (hydro.getEcoliCfu100ml() > 100) measurementRisk += 25;
                else if (hydro.getEcoliCfu100ml() > 10) measurementRisk += 15;
                else measurementRisk += 5;
            }

            // Cap individual measurement risk at 100
            totalRisk += Math.min(measurementRisk, 100);
            validMeasurements++;
        }

        return validMeasurements > 0 ? totalRisk / validMeasurements : 50;
    }

    /**
     * Access Distance Score (30% weight)
     * Measures proximity to water sources and facilities.
     */
    private int calculateAccessDistanceScore(CommunityData community) {
        // Find nearest water source (hydro measurement or infrastructure)
        List<HydroData> nearbyMeasurements = hydroDataRepository.findNearest(
            community.getCoordinates().getX(),
            community.getCoordinates().getY(),
            5
        );

        List<InfrastructureData> nearbyFacilities = infrastructureDataRepository.findNearest(
            community.getCoordinates().getX(),
            community.getCoordinates().getY(),
            5
        );

        // Calculate distance to nearest source
        double minDistance = Double.MAX_VALUE;

        for (HydroData hydro : nearbyMeasurements) {
            double distance = calculateDistance(community, hydro.getCoordinates());
            minDistance = Math.min(minDistance, distance);
        }

        for (InfrastructureData facility : nearbyFacilities) {
            double distance = calculateDistance(community, facility.getCoordinates());
            minDistance = Math.min(minDistance, distance);
        }

        // Score based on distance thresholds
        if (minDistance == Double.MAX_VALUE) {
            return 100;  // No water sources found = critical risk
        } else if (minDistance < SAFE_DISTANCE) {
            return 10;   // <500m = low risk
        } else if (minDistance < MODERATE_DISTANCE) {
            // Linear interpolation 500m-1500m -> 10-50 risk
            return (int) (10 + ((minDistance - SAFE_DISTANCE) / (MODERATE_DISTANCE - SAFE_DISTANCE)) * 40);
        } else if (minDistance < CRITICAL_DISTANCE) {
            // Linear interpolation 1500m-5000m -> 50-90 risk
            return (int) (50 + ((minDistance - MODERATE_DISTANCE) / (CRITICAL_DISTANCE - MODERATE_DISTANCE)) * 40);
        } else {
            return 95;   // >5km = very high risk
        }
    }

    /**
     * Infrastructure Score (25% weight)
     * Evaluates nearby facility reliability and operational status.
     */
    private int calculateInfrastructureScore(CommunityData community) {
        List<InfrastructureData> facilities = infrastructureDataRepository.findWithinRadius(
            community.getCoordinates().getX(),
            community.getCoordinates().getY(),
            5000.0  // 5km radius
        );

        if (facilities.isEmpty()) {
            return 80;  // No infrastructure = high risk
        }

        // Count operational vs non-operational facilities
        long operational = facilities.stream()
            .filter(f -> "OPERATIONAL".equalsIgnoreCase(f.getOperationalStatus()))
            .count();

        long total = facilities.size();

        // Calculate operational ratio
        double operationalRatio = (double) operational / total;

        // Score inversely proportional to operational ratio
        if (operationalRatio >= 0.8) {
            return 15;  // 80%+ operational = low risk
        } else if (operationalRatio >= 0.5) {
            return 40;  // 50-80% operational = moderate risk
        } else if (operationalRatio >= 0.2) {
            return 70;  // 20-50% operational = high risk
        } else {
            return 90;  // <20% operational = critical risk
        }
    }

    /**
     * Population Pressure Score (10% weight)
     * Analyzes demand vs capacity based on population and access levels.
     */
    private int calculatePopulationPressureScore(CommunityData community) {
        Integer population = community.getPopulation();

        if (population == null || population == 0) {
            return 30;  // Unknown population = default low-moderate risk
        }

        // Count nearby operational facilities
        List<InfrastructureData> facilities = infrastructureDataRepository.findWithinRadius(
            community.getCoordinates().getX(),
            community.getCoordinates().getY(),
            2000.0  // 2km radius for local capacity
        );

        long operationalFacilities = facilities.stream()
            .filter(f -> "OPERATIONAL".equalsIgnoreCase(f.getOperationalStatus()))
            .count();

        // Calculate people per facility
        if (operationalFacilities == 0) {
            return 85;  // No facilities = high pressure
        }

        double peoplePerFacility = (double) population / operationalFacilities;

        // Score based on WHO recommendation (1 facility per 250 people)
        if (peoplePerFacility < 250) {
            return 10;   // Low pressure
        } else if (peoplePerFacility < 500) {
            return 30;   // Moderate pressure
        } else if (peoplePerFacility < 1000) {
            return 60;   // High pressure
        } else {
            return 90;   // Critical pressure
        }
    }

    /**
     * Determines risk level based on overall score.
     */
    private RiskAssessment.RiskLevel determineRiskLevel(int score) {
        if (score >= 67) {
            return RiskAssessment.RiskLevel.HIGH;
        } else if (score >= 34) {
            return RiskAssessment.RiskLevel.MEDIUM;
        } else {
            return RiskAssessment.RiskLevel.LOW;
        }
    }

    /**
     * Determines confidence level based on sample count.
     */
    private RiskAssessment.ConfidenceLevel determineConfidenceLevel(int sampleCount) {
        if (sampleCount > 30) {
            return RiskAssessment.ConfidenceLevel.HIGH;
        } else if (sampleCount >= 10) {
            return RiskAssessment.ConfidenceLevel.MEDIUM;
        } else if (sampleCount >= 1) {
            return RiskAssessment.ConfidenceLevel.LOW;
        } else {
            return RiskAssessment.ConfidenceLevel.NONE;
        }
    }

    /**
     * Counts water quality measurements within 5km of community.
     */
    private int countNearbyMeasurements(CommunityData community) {
        List<HydroData> measurements = hydroDataRepository.findWithinRadius(
            community.getCoordinates().getX(),
            community.getCoordinates().getY(),
            5000.0
        );
        return measurements.size();
    }

    /**
     * Generates JSON explanation of top 3 contributing factors.
     */
    private String generateExplanation(int waterQuality, int accessDistance,
                                      int infrastructure, int population) {
        // Create list of factors with their weighted contributions
        List<Map<String, Object>> factors = new ArrayList<>();

        factors.add(Map.of(
            "factor", "Water Quality",
            "score", waterQuality,
            "weight", WATER_QUALITY_WEIGHT,
            "contribution", waterQuality * WATER_QUALITY_WEIGHT
        ));

        factors.add(Map.of(
            "factor", "Access Distance",
            "score", accessDistance,
            "weight", ACCESS_DISTANCE_WEIGHT,
            "contribution", accessDistance * ACCESS_DISTANCE_WEIGHT
        ));

        factors.add(Map.of(
            "factor", "Infrastructure",
            "score", infrastructure,
            "weight", INFRASTRUCTURE_WEIGHT,
            "contribution", infrastructure * INFRASTRUCTURE_WEIGHT
        ));

        factors.add(Map.of(
            "factor", "Population Pressure",
            "score", population,
            "weight", POPULATION_PRESSURE_WEIGHT,
            "contribution", population * POPULATION_PRESSURE_WEIGHT
        ));

        // Sort by contribution (descending) and take top 3
        List<Map<String, Object>> topFactors = factors.stream()
            .sorted((a, b) -> Double.compare(
                (Double) b.get("contribution"),
                (Double) a.get("contribution")
            ))
            .limit(3)
            .collect(Collectors.toList());

        // Build JSON manually (simple format)
        StringBuilder json = new StringBuilder("[");
        for (int i = 0; i < topFactors.size(); i++) {
            Map<String, Object> factor = topFactors.get(i);
            if (i > 0) json.append(",");
            json.append(String.format(
                "{\"factor\":\"%s\",\"score\":%d,\"weight\":%.2f,\"contribution\":%.2f}",
                factor.get("factor"),
                factor.get("score"),
                factor.get("weight"),
                factor.get("contribution")
            ));
        }
        json.append("]");

        return json.toString();
    }

    /**
     * Calculates distance between community and point (Haversine formula).
     * Returns distance in meters.
     */
    private double calculateDistance(CommunityData community, org.locationtech.jts.geom.Point point) {
        double lat1 = Math.toRadians(community.getCoordinates().getY());
        double lon1 = Math.toRadians(community.getCoordinates().getX());
        double lat2 = Math.toRadians(point.getY());
        double lon2 = Math.toRadians(point.getX());

        double dLat = lat2 - lat1;
        double dLon = lon2 - lon1;

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(lat1) * Math.cos(lat2) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        final double EARTH_RADIUS_METERS = 6371000;
        return EARTH_RADIUS_METERS * c;
    }

    /**
     * Result object containing all risk score components.
     */
    @lombok.Builder
    @lombok.Data
    public static class RiskScoreResult {
        private Long communityId;
        private Integer overallScore;
        private RiskAssessment.RiskLevel riskLevel;
        private Integer waterQualityScore;
        private Integer accessDistanceScore;
        private Integer infrastructureScore;
        private Integer populationPressureScore;
        private RiskAssessment.ConfidenceLevel confidenceLevel;
        private Integer sampleCount;
        private String explanationJson;
    }
}
