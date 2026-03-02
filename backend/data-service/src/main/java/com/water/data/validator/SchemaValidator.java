package com.water.data.validator;

import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Stage 2 Validation: Schema Validation
 * Validates CSV structure - required columns, header format, column mapping.
 */
@Component
public class SchemaValidator {

    /**
     * Validates CSV schema against required columns for the data type.
     *
     * @param headers Array of CSV column headers
     * @param dataType Type of data being uploaded (hydro, community, infrastructure)
     * @return Validation result with success status and error messages
     */
    public SchemaValidationResult validateSchema(String[] headers, DataType dataType) {
        List<String> errors = new ArrayList<>();

        // Normalize headers (lowercase, trim spaces, replace spaces with underscores)
        Map<String, String> normalizedHeaders = normalizeHeaders(headers);

        // Get required columns for this data type
        Set<String> requiredColumns = getRequiredColumns(dataType);

        // Check for missing required columns
        List<String> missingColumns = new ArrayList<>();
        for (String required : requiredColumns) {
            if (!normalizedHeaders.containsKey(required)) {
                missingColumns.add(required);
            }
        }

        if (!missingColumns.isEmpty()) {
            errors.add(String.format(
                "Missing required columns: %s. Expected columns for %s data: %s",
                String.join(", ", missingColumns),
                dataType.name().toLowerCase(),
                String.join(", ", requiredColumns)
            ));
        }

        // Create column mapping for validated schema
        Map<String, Integer> columnIndexMap = new HashMap<>();
        for (int i = 0; i < headers.length; i++) {
            String normalized = normalizeColumnName(headers[i]);
            columnIndexMap.put(normalized, i);
        }

        return new SchemaValidationResult(
            errors.isEmpty(),
            errors,
            normalizedHeaders,
            columnIndexMap
        );
    }

    /**
     * Normalizes column headers to standard format.
     * Example: "Community Name" → "community_name"
     */
    private Map<String, String> normalizeHeaders(String[] rawHeaders) {
        Map<String, String> headerMap = new HashMap<>();

        for (String header : rawHeaders) {
            String normalized = normalizeColumnName(header);
            headerMap.put(normalized, header); // Map normalized → original
        }

        return headerMap;
    }

    /**
     * Normalizes a single column name.
     */
    private String normalizeColumnName(String columnName) {
        return columnName.trim()
            .toLowerCase()
            .replace(" ", "_")
            .replace("-", "_");
    }

    /**
     * Gets required columns for a data type.
     */
    private Set<String> getRequiredColumns(DataType dataType) {
        return switch (dataType) {
            case HYDRO -> Set.of(
                "source",
                "latitude",
                "longitude",
                "measurement_value",
                "measurement_unit",
                "measurement_date"
            );
            case COMMUNITY -> Set.of(
                "community_name",
                "latitude",
                "longitude",
                "population"
            );
            case INFRASTRUCTURE -> Set.of(
                "facility_type",
                "facility_name",
                "latitude",
                "longitude",
                "operational_status"
            );
        };
    }

    /**
     * Gets optional columns for a data type (for completeness scoring).
     */
    public Set<String> getOptionalColumns(DataType dataType) {
        return switch (dataType) {
            case HYDRO -> Set.of(
                "data_type",
                "location_name",
                "depth_meters",
                "parameter_name",
                "notes"
            );
            case COMMUNITY -> Set.of(
                "water_access_level",
                "primary_water_source",
                "household_count",
                "collection_date",
                "source",
                "notes"
            );
            case INFRASTRUCTURE -> Set.of(
                "capacity",
                "capacity_unit",
                "population_served",
                "installation_date",
                "last_maintenance_date",
                "source",
                "notes"
            );
        };
    }

    /**
     * Data types supported for upload.
     */
    public enum DataType {
        HYDRO,
        COMMUNITY,
        INFRASTRUCTURE
    }

    /**
     * Result of schema validation.
     */
    public static class SchemaValidationResult {
        private final boolean valid;
        private final List<String> errors;
        private final Map<String, String> normalizedHeaders;
        private final Map<String, Integer> columnIndexMap;

        public SchemaValidationResult(
            boolean valid,
            List<String> errors,
            Map<String, String> normalizedHeaders,
            Map<String, Integer> columnIndexMap
        ) {
            this.valid = valid;
            this.errors = errors;
            this.normalizedHeaders = normalizedHeaders;
            this.columnIndexMap = columnIndexMap;
        }

        public boolean isValid() {
            return valid;
        }

        public List<String> getErrors() {
            return errors;
        }

        public Map<String, String> getNormalizedHeaders() {
            return normalizedHeaders;
        }

        public Map<String, Integer> getColumnIndexMap() {
            return columnIndexMap;
        }

        public String getErrorSummary() {
            return String.join("; ", errors);
        }
    }
}
