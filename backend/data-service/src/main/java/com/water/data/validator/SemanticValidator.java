package com.water.data.validator;

import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Stage 4 Validation: Semantic Validation
 * Validates cross-field consistency, duplicates, and geographic clustering.
 */
@Component
public class SemanticValidator {

    // Threshold for geographic outlier detection (km)
    private static final double OUTLIER_DISTANCE_THRESHOLD_KM = 50.0;

    /**
     * Validates semantic consistency across all rows.
     *
     * @param rows List of validated rows
     * @param dataType Type of data being validated
     * @return Semantic validation result with warnings
     */
    public SemanticValidationResult validateSemantic(
        List<Map<String, String>> rows,
        SchemaValidator.DataType dataType
    ) {
        List<ValidationWarning> warnings = new ArrayList<>();

        if (rows.isEmpty()) {
            return new SemanticValidationResult(warnings);
        }

        // 1. Duplicate detection
        warnings.addAll(detectDuplicates(rows, dataType));

        // 2. Geographic clustering (check for outliers)
        warnings.addAll(detectGeographicOutliers(rows));

        // 3. Temporal consistency (dates should be ordered)
        if (dataType == SchemaValidator.DataType.HYDRO) {
            warnings.addAll(validateTemporalConsistency(rows));
        }

        return new SemanticValidationResult(warnings);
    }

    /**
     * Detects exact and near duplicates.
     */
    private List<ValidationWarning> detectDuplicates(
        List<Map<String, String>> rows,
        SchemaValidator.DataType dataType
    ) {
        List<ValidationWarning> warnings = new ArrayList<>();
        Map<String, List<Integer>> duplicateMap = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String key = generateDuplicateKey(row, dataType);

            duplicateMap.computeIfAbsent(key, k -> new ArrayList<>()).add(i + 2); // +2 for 1-indexed and header
        }

        // Report duplicates
        for (Map.Entry<String, List<Integer>> entry : duplicateMap.entrySet()) {
            if (entry.getValue().size() > 1) {
                String duplicateRows = entry.getValue().stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(", "));

                warnings.add(new ValidationWarning(
                    entry.getValue().get(0),
                    "duplicate",
                    String.format(
                        "Duplicate detected: Rows %s have identical key fields. " +
                        "Only the first occurrence will be imported.",
                        duplicateRows
                    ),
                    "Review if these are truly duplicates or if data needs correction."
                ));
            }
        }

        return warnings;
    }

    /**
     * Generates a duplicate detection key based on data type.
     */
    private String generateDuplicateKey(Map<String, String> row, SchemaValidator.DataType dataType) {
        return switch (dataType) {
            case HYDRO -> String.format("%s|%s|%s|%s",
                row.get("latitude"),
                row.get("longitude"),
                row.get("parameter_name"),
                row.get("measurement_date")
            );
            case COMMUNITY -> String.format("%s|%s|%s",
                row.get("community_name"),
                row.get("latitude"),
                row.get("longitude")
            );
            case INFRASTRUCTURE -> String.format("%s|%s|%s",
                row.get("facility_name"),
                row.get("latitude"),
                row.get("longitude")
            );
        };
    }

    /**
     * Detects geographic outliers (points far from dataset centroid).
     */
    private List<ValidationWarning> detectGeographicOutliers(List<Map<String, String>> rows) {
        List<ValidationWarning> warnings = new ArrayList<>();

        if (rows.size() < 3) {
            return warnings; // Need at least 3 points for meaningful clustering
        }

        // Calculate centroid
        double sumLat = 0;
        double sumLon = 0;
        int validCoords = 0;

        for (Map<String, String> row : rows) {
            try {
                double lat = Double.parseDouble(row.get("latitude"));
                double lon = Double.parseDouble(row.get("longitude"));
                sumLat += lat;
                sumLon += lon;
                validCoords++;
            } catch (NumberFormatException | NullPointerException e) {
                // Skip invalid coordinates
            }
        }

        if (validCoords < 3) {
            return warnings;
        }

        double centroidLat = sumLat / validCoords;
        double centroidLon = sumLon / validCoords;

        // Check each point against centroid
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                double lat = Double.parseDouble(row.get("latitude"));
                double lon = Double.parseDouble(row.get("longitude"));

                double distanceKm = calculateDistance(lat, lon, centroidLat, centroidLon);

                if (distanceKm > OUTLIER_DISTANCE_THRESHOLD_KM) {
                    warnings.add(new ValidationWarning(
                        i + 2, // +2 for 1-indexed and header
                        "geographic_outlier",
                        String.format(
                            "This location (%.4f°N, %.4f°E) is %.1f km from other data points in this file. " +
                            "Verify coordinates are correct.",
                            lat, lon, distanceKm
                        ),
                        "Check if this point should be in a different dataset or if coordinates were entered incorrectly."
                    ));
                }
            } catch (NumberFormatException | NullPointerException e) {
                // Skip invalid coordinates (already caught in row validation)
            }
        }

        return warnings;
    }

    /**
     * Validates temporal consistency (measurements at same location should have ordered dates).
     */
    private List<ValidationWarning> validateTemporalConsistency(List<Map<String, String>> rows) {
        List<ValidationWarning> warnings = new ArrayList<>();

        // Group by location (lat/lon)
        Map<String, List<Integer>> locationGroups = new HashMap<>();

        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            String locationKey = String.format("%s|%s",
                row.get("latitude"),
                row.get("longitude")
            );

            locationGroups.computeIfAbsent(locationKey, k -> new ArrayList<>()).add(i);
        }

        // Check temporal ordering for each location with multiple measurements
        for (Map.Entry<String, List<Integer>> entry : locationGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                List<Integer> indices = entry.getValue();

                // Check if dates are in descending order (later measurements appearing before earlier ones)
                for (int i = 0; i < indices.size() - 1; i++) {
                    Map<String, String> row1 = rows.get(indices.get(i));
                    Map<String, String> row2 = rows.get(indices.get(i + 1));

                    String date1 = row1.get("measurement_date");
                    String date2 = row2.get("measurement_date");

                    // Simple string comparison works for ISO 8601 dates
                    if (date1 != null && date2 != null && date1.compareTo(date2) > 0) {
                        warnings.add(new ValidationWarning(
                            indices.get(i) + 2,
                            "temporal_inconsistency",
                            String.format(
                                "Measurements at same location (rows %d and %d) appear out of chronological order. " +
                                "Earlier date (%s) appears after later date (%s).",
                                indices.get(i) + 2, indices.get(i + 1) + 2, date2, date1
                            ),
                            "Consider sorting data by date for better time-series analysis."
                        ));
                        break; // Only warn once per location
                    }
                }
            }
        }

        return warnings;
    }

    /**
     * Calculates great-circle distance between two points (Haversine formula).
     *
     * @param lat1 Latitude of point 1 (degrees)
     * @param lon1 Longitude of point 1 (degrees)
     * @param lat2 Latitude of point 2 (degrees)
     * @param lon2 Longitude of point 2 (degrees)
     * @return Distance in kilometers
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371.0; // Earth radius in kilometers

        double lat1Rad = Math.toRadians(lat1);
        double lat2Rad = Math.toRadians(lat2);
        double deltaLat = Math.toRadians(lat2 - lat1);
        double deltaLon = Math.toRadians(lon2 - lon1);

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                   Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                   Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);

        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return R * c;
    }

    /**
     * Result of semantic validation.
     */
    public static class SemanticValidationResult {
        private final List<ValidationWarning> warnings;

        public SemanticValidationResult(List<ValidationWarning> warnings) {
            this.warnings = warnings;
        }

        public List<ValidationWarning> getWarnings() {
            return warnings;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }
    }

    /**
     * Semantic validation warning (not an error, but something to review).
     */
    public static class ValidationWarning {
        private final int row;
        private final String type; // duplicate, geographic_outlier, temporal_inconsistency
        private final String message;
        private final String suggestion;

        public ValidationWarning(int row, String type, String message, String suggestion) {
            this.row = row;
            this.type = type;
            this.message = message;
            this.suggestion = suggestion;
        }

        public int getRow() {
            return row;
        }

        public String getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }

        public String getSuggestion() {
            return suggestion;
        }
    }
}
