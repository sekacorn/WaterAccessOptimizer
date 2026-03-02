package com.water.data.validator;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 3 Validation: Row-Level Validation
 * Validates individual CSV rows against business rules, data types, ranges, and WHO guidelines.
 */
@Component
public class RowValidator {

    // WHO Water Quality Guidelines (for reference and validation)
    private static final Map<String, Double> WHO_GUIDELINES = Map.of(
        "arsenic", 10.0,      // µg/L
        "fluoride", 1.5,      // mg/L
        "nitrate", 50.0,      // mg/L
        "lead", 10.0,         // µg/L
        "turbidity", 5.0      // NTU
    );

    // Valid enum values for validation
    private static final Set<String> VALID_ACCESS_LEVELS = Set.of(
        "none", "limited", "basic", "safely_managed"
    );

    private static final Set<String> VALID_WATER_SOURCES = Set.of(
        "well", "borehole", "piped_water", "surface_water", "rainwater", "vendor", "other"
    );

    private static final Set<String> VALID_FACILITY_TYPES = Set.of(
        "well", "borehole", "treatment_plant", "reservoir",
        "distribution_point", "pump_station", "water_tower",
        "spring_protection", "other"
    );

    private static final Set<String> VALID_OPERATIONAL_STATUS = Set.of(
        "operational", "non_operational", "under_maintenance", "planned", "abandoned"
    );

    /**
     * Validates a hydro data row.
     */
    public RowValidationResult validateHydroRow(Map<String, String> row, int rowNumber) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate coordinates
        errors.addAll(validateCoordinates(row, rowNumber));

        // Validate measurement_value
        try {
            double value = Double.parseDouble(row.get("measurement_value"));
            String parameterName = row.get("parameter_name");

            // Check WHO guidelines for known parameters
            if (parameterName != null && WHO_GUIDELINES.containsKey(parameterName.toLowerCase())) {
                double guideline = WHO_GUIDELINES.get(parameterName.toLowerCase());
                if (value > guideline) {
                    double exceedanceFactor = value / guideline;
                    errors.add(new ValidationError(
                        rowNumber,
                        "measurement_value",
                        String.format("%.2f", value),
                        String.format(
                            "%s level (%.2f) exceeds WHO guideline (%.2f) by %.1fx. " +
                            "This may indicate contaminated water requiring urgent attention.",
                            capitalize(parameterName), value, guideline, exceedanceFactor
                        ),
                        ValidationSeverity.INFO,  // INFO, not ERROR - still valid data
                        "Consider testing additional samples to confirm contamination level."
                    ));
                }
            }

        } catch (NumberFormatException e) {
            errors.add(new ValidationError(
                rowNumber,
                "measurement_value",
                row.get("measurement_value"),
                "Measurement value must be a number.",
                ValidationSeverity.ERROR,
                "Ensure the value is numeric (e.g., 125.5, not 'high' or 'N/A')."
            ));
        } catch (NullPointerException e) {
            errors.add(new ValidationError(
                rowNumber,
                "measurement_value",
                null,
                "Measurement value is required.",
                ValidationSeverity.ERROR,
                "Add a numeric value for this measurement."
            ));
        }

        // Validate measurement_unit
        String unit = row.get("measurement_unit");
        if (unit == null || unit.trim().isEmpty()) {
            errors.add(new ValidationError(
                rowNumber,
                "measurement_unit",
                null,
                "Missing required field 'measurement_unit'.",
                ValidationSeverity.ERROR,
                "Add a unit for this measurement (e.g., 'ppm', 'mg/L', 'µg/L', 'meters', 'pH', 'NTU')."
            ));
        }

        // Validate measurement_date
        errors.addAll(validateDate(row, "measurement_date", rowNumber));

        // Validate source
        String source = row.get("source");
        if (source == null || source.trim().isEmpty()) {
            errors.add(new ValidationError(
                rowNumber,
                "source",
                null,
                "Missing required field 'source'.",
                ValidationSeverity.ERROR,
                "Specify the data source (e.g., 'Field Survey', 'USGS', 'WHO')."
            ));
        }

        return new RowValidationResult(rowNumber, errors);
    }

    /**
     * Validates a community data row.
     */
    public RowValidationResult validateCommunityRow(Map<String, String> row, int rowNumber) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate coordinates
        errors.addAll(validateCoordinates(row, rowNumber));

        // Validate community_name
        String name = row.get("community_name");
        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError(
                rowNumber,
                "community_name",
                null,
                "Missing required field 'community_name'.",
                ValidationSeverity.ERROR,
                "Add a name for this community."
            ));
        }

        // Validate population
        try {
            int population = Integer.parseInt(row.get("population"));
            if (population <= 0) {
                errors.add(new ValidationError(
                    rowNumber,
                    "population",
                    String.valueOf(population),
                    "Population must be greater than 0.",
                    ValidationSeverity.ERROR,
                    "Ensure population value is a positive integer."
                ));
            }

            // Validate population/household ratio (2-10 is typical)
            String householdStr = row.get("household_count");
            if (householdStr != null && !householdStr.trim().isEmpty()) {
                try {
                    int households = Integer.parseInt(householdStr);
                    if (households > 0) {
                        double ratio = (double) population / households;
                        if (ratio < 2 || ratio > 10) {
                            errors.add(new ValidationError(
                                rowNumber,
                                "household_count",
                                householdStr,
                                String.format(
                                    "Population/household ratio (%.1f) is unusual. " +
                                    "Typical range is 2-10 people per household.",
                                    ratio
                                ),
                                ValidationSeverity.WARNING,
                                "Verify population and household count are correct."
                            ));
                        }
                    }
                } catch (NumberFormatException e) {
                    errors.add(new ValidationError(
                        rowNumber,
                        "household_count",
                        householdStr,
                        "Household count must be a number.",
                        ValidationSeverity.ERROR,
                        "Use a numeric value for household count."
                    ));
                }
            }

        } catch (NumberFormatException e) {
            errors.add(new ValidationError(
                rowNumber,
                "population",
                row.get("population"),
                "Population must be an integer.",
                ValidationSeverity.ERROR,
                "Ensure population is a whole number (e.g., 1200, not 1200.5)."
            ));
        } catch (NullPointerException e) {
            errors.add(new ValidationError(
                rowNumber,
                "population",
                null,
                "Missing required field 'population'.",
                ValidationSeverity.ERROR,
                "Add a population value for this community."
            ));
        }

        // Validate water_access_level
        String accessLevel = row.get("water_access_level");
        if (accessLevel != null && !accessLevel.trim().isEmpty()) {
            if (!VALID_ACCESS_LEVELS.contains(accessLevel.toLowerCase())) {
                errors.add(new ValidationError(
                    rowNumber,
                    "water_access_level",
                    accessLevel,
                    String.format(
                        "Invalid water_access_level: '%s'. Must be one of: %s (WHO JMP Service Ladder).",
                        accessLevel,
                        String.join(", ", VALID_ACCESS_LEVELS)
                    ),
                    ValidationSeverity.ERROR,
                    "Use a valid WHO JMP service level. See documentation for definitions."
                ));
            }
        }

        // Validate primary_water_source
        String waterSource = row.get("primary_water_source");
        if (waterSource != null && !waterSource.trim().isEmpty()) {
            if (!VALID_WATER_SOURCES.contains(waterSource.toLowerCase())) {
                errors.add(new ValidationError(
                    rowNumber,
                    "primary_water_source",
                    waterSource,
                    String.format(
                        "Unknown water source: '%s'. Known sources: %s. Using 'other' is acceptable.",
                        waterSource,
                        String.join(", ", VALID_WATER_SOURCES)
                    ),
                    ValidationSeverity.WARNING,
                    "Consider using a standard water source type or 'other'."
                ));
            }
        }

        return new RowValidationResult(rowNumber, errors);
    }

    /**
     * Validates an infrastructure data row.
     */
    public RowValidationResult validateInfrastructureRow(Map<String, String> row, int rowNumber) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate coordinates
        errors.addAll(validateCoordinates(row, rowNumber));

        // Validate facility_type
        String facilityType = row.get("facility_type");
        if (facilityType == null || facilityType.trim().isEmpty()) {
            errors.add(new ValidationError(
                rowNumber,
                "facility_type",
                null,
                "Missing required field 'facility_type'.",
                ValidationSeverity.ERROR,
                "Specify the type of facility (e.g., 'borehole', 'well', 'treatment_plant')."
            ));
        } else if (!VALID_FACILITY_TYPES.contains(facilityType.toLowerCase())) {
            errors.add(new ValidationError(
                rowNumber,
                "facility_type",
                facilityType,
                String.format(
                    "Invalid facility_type: '%s'. Must be one of: %s.",
                    facilityType,
                    String.join(", ", VALID_FACILITY_TYPES)
                ),
                ValidationSeverity.ERROR,
                "Use a valid facility type. 'other' is acceptable if none match."
            ));
        }

        // Validate facility_name
        String name = row.get("facility_name");
        if (name == null || name.trim().isEmpty()) {
            errors.add(new ValidationError(
                rowNumber,
                "facility_name",
                null,
                "Missing required field 'facility_name'.",
                ValidationSeverity.ERROR,
                "Add a name for this facility."
            ));
        }

        // Validate operational_status
        String status = row.get("operational_status");
        if (status == null || status.trim().isEmpty()) {
            errors.add(new ValidationError(
                rowNumber,
                "operational_status",
                null,
                "Missing required field 'operational_status'.",
                ValidationSeverity.ERROR,
                "Specify operational status (e.g., 'operational', 'non_operational')."
            ));
        } else if (!VALID_OPERATIONAL_STATUS.contains(status.toLowerCase())) {
            errors.add(new ValidationError(
                rowNumber,
                "operational_status",
                status,
                String.format(
                    "Invalid operational_status: '%s'. Must be one of: %s.",
                    status,
                    String.join(", ", VALID_OPERATIONAL_STATUS)
                ),
                ValidationSeverity.ERROR,
                "Use a valid operational status."
            ));
        }

        // Validate capacity (if provided)
        String capacityStr = row.get("capacity");
        if (capacityStr != null && !capacityStr.trim().isEmpty()) {
            try {
                double capacity = Double.parseDouble(capacityStr);
                if (capacity <= 0) {
                    errors.add(new ValidationError(
                        rowNumber,
                        "capacity",
                        capacityStr,
                        "Capacity must be greater than 0.",
                        ValidationSeverity.ERROR,
                        "Ensure capacity is a positive number."
                    ));
                }

                // Check if capacity_unit is provided when capacity is provided
                String capacityUnit = row.get("capacity_unit");
                if (capacityUnit == null || capacityUnit.trim().isEmpty()) {
                    errors.add(new ValidationError(
                        rowNumber,
                        "capacity_unit",
                        null,
                        "capacity_unit is required when capacity is provided.",
                        ValidationSeverity.ERROR,
                        "Add a unit (e.g., 'liters_per_day', 'liters', 'cubic_meters')."
                    ));
                }

            } catch (NumberFormatException e) {
                errors.add(new ValidationError(
                    rowNumber,
                    "capacity",
                    capacityStr,
                    "Capacity must be a number.",
                    ValidationSeverity.ERROR,
                    "Use a numeric value for capacity."
                ));
            }
        }

        // Warn if operational but no capacity
        if ("operational".equalsIgnoreCase(status) &&
            (capacityStr == null || capacityStr.trim().isEmpty())) {
            errors.add(new ValidationError(
                rowNumber,
                "capacity",
                null,
                "Operational facilities should have capacity specified.",
                ValidationSeverity.WARNING,
                "Add capacity information for better risk assessment accuracy."
            ));
        }

        return new RowValidationResult(rowNumber, errors);
    }

    /**
     * Validates latitude and longitude fields.
     */
    private List<ValidationError> validateCoordinates(Map<String, String> row, int rowNumber) {
        List<ValidationError> errors = new ArrayList<>();

        // Validate latitude
        try {
            double lat = Double.parseDouble(row.get("latitude"));
            if (lat < -90 || lat > 90) {
                errors.add(new ValidationError(
                    rowNumber,
                    "latitude",
                    String.format("%.6f", lat),
                    String.format("Invalid latitude: %.6f. Must be between -90 and 90.", lat),
                    ValidationSeverity.ERROR,
                    "Check if latitude and longitude are swapped."
                ));
            }
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(
                rowNumber,
                "latitude",
                row.get("latitude"),
                "Latitude must be a number.",
                ValidationSeverity.ERROR,
                "Use decimal degrees format (e.g., 0.3476, not '0°20\\'51\"')."
            ));
        } catch (NullPointerException e) {
            errors.add(new ValidationError(
                rowNumber,
                "latitude",
                null,
                "Missing required field 'latitude'.",
                ValidationSeverity.ERROR,
                "Add latitude coordinate in decimal degrees."
            ));
        }

        // Validate longitude
        try {
            double lon = Double.parseDouble(row.get("longitude"));
            if (lon < -180 || lon > 180) {
                errors.add(new ValidationError(
                    rowNumber,
                    "longitude",
                    String.format("%.6f", lon),
                    String.format("Invalid longitude: %.6f. Must be between -180 and 180.", lon),
                    ValidationSeverity.ERROR,
                    "Check if latitude and longitude are swapped."
                ));
            }
        } catch (NumberFormatException e) {
            errors.add(new ValidationError(
                rowNumber,
                "longitude",
                row.get("longitude"),
                "Longitude must be a number.",
                ValidationSeverity.ERROR,
                "Use decimal degrees format (e.g., 32.5825, not '32°34\\'57\"')."
            ));
        } catch (NullPointerException e) {
            errors.add(new ValidationError(
                rowNumber,
                "longitude",
                null,
                "Missing required field 'longitude'.",
                ValidationSeverity.ERROR,
                "Add longitude coordinate in decimal degrees."
            ));
        }

        return errors;
    }

    /**
     * Validates date fields (supports ISO 8601 formats).
     */
    private List<ValidationError> validateDate(Map<String, String> row, String field, int rowNumber) {
        List<ValidationError> errors = new ArrayList<>();
        String dateStr = row.get(field);

        if (dateStr == null || dateStr.trim().isEmpty()) {
            errors.add(new ValidationError(
                rowNumber,
                field,
                null,
                String.format("Missing required field '%s'.", field),
                ValidationSeverity.ERROR,
                "Use ISO 8601 format: YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS"
            ));
            return errors;
        }

        try {
            // Try parsing as LocalDateTime first (with time)
            LocalDateTime dateTime = LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);

            // Check if date is in the future
            if (dateTime.isAfter(LocalDateTime.now())) {
                errors.add(new ValidationError(
                    rowNumber,
                    field,
                    dateStr,
                    String.format("Date '%s' is in the future. Measurements should be historical.", dateStr),
                    ValidationSeverity.WARNING,
                    "Check the date is correct."
                ));
            }

        } catch (DateTimeParseException e1) {
            try {
                // Try parsing as LocalDate (date only)
                LocalDate date = LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);

                // Check if date is in the future
                if (date.isAfter(LocalDate.now())) {
                    errors.add(new ValidationError(
                        rowNumber,
                        field,
                        dateStr,
                        String.format("Date '%s' is in the future. Measurements should be historical.", dateStr),
                        ValidationSeverity.WARNING,
                        "Check the date is correct."
                    ));
                }

            } catch (DateTimeParseException e2) {
                errors.add(new ValidationError(
                    rowNumber,
                    field,
                    dateStr,
                    String.format(
                        "Invalid date format: '%s'. Use ISO 8601 format: YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS",
                        dateStr
                    ),
                    ValidationSeverity.ERROR,
                    "Example valid dates: 2024-01-15 or 2024-01-15T10:30:00"
                ));
            }
        }

        return errors;
    }

    /**
     * Capitalizes first letter of a string.
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }

    /**
     * Result of row validation.
     */
    public static class RowValidationResult {
        private final int rowNumber;
        private final List<ValidationError> errors;

        public RowValidationResult(int rowNumber, List<ValidationError> errors) {
            this.rowNumber = rowNumber;
            this.errors = errors;
        }

        public int getRowNumber() {
            return rowNumber;
        }

        public List<ValidationError> getErrors() {
            return errors;
        }

        public boolean hasErrors() {
            return errors.stream().anyMatch(e -> e.getSeverity() == ValidationSeverity.ERROR);
        }

        public boolean hasWarnings() {
            return errors.stream().anyMatch(e -> e.getSeverity() == ValidationSeverity.WARNING);
        }

        public long getErrorCount() {
            return errors.stream().filter(e -> e.getSeverity() == ValidationSeverity.ERROR).count();
        }

        public long getWarningCount() {
            return errors.stream().filter(e -> e.getSeverity() == ValidationSeverity.WARNING).count();
        }
    }

    /**
     * Validation error with severity and suggestion.
     */
    public static class ValidationError {
        private final int row;
        private final String field;
        private final String value;
        private final String message;
        private final ValidationSeverity severity;
        private final String suggestion;

        public ValidationError(
            int row,
            String field,
            String value,
            String message,
            ValidationSeverity severity,
            String suggestion
        ) {
            this.row = row;
            this.field = field;
            this.value = value;
            this.message = message;
            this.severity = severity;
            this.suggestion = suggestion;
        }

        public int getRow() {
            return row;
        }

        public String getField() {
            return field;
        }

        public String getValue() {
            return value;
        }

        public String getMessage() {
            return message;
        }

        public ValidationSeverity getSeverity() {
            return severity;
        }

        public String getSuggestion() {
            return suggestion;
        }
    }

    /**
     * Validation severity levels.
     */
    public enum ValidationSeverity {
        ERROR,    // Prevents import
        WARNING,  // Import but flag for review
        INFO      // Informational only
    }
}
