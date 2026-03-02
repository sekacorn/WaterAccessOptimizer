package com.water.data.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Validation error DTO for API responses.
 * Matches error format defined in docs/DATA_INGESTION_GUIDE.md
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ValidationErrorDto {

    private Integer row;
    private String field;
    private String value;
    private String message;
    private String severity;  // "error", "warning", "info"
    private String suggestion;

    // Constructors
    public ValidationErrorDto() {}

    public ValidationErrorDto(
        int row,
        String field,
        String value,
        String message,
        String severity,
        String suggestion
    ) {
        this.row = row;
        this.field = field;
        this.value = value;
        this.message = message;
        this.severity = severity;
        this.suggestion = suggestion;
    }

    // Static factory methods
    public static ValidationErrorDto error(
        int row,
        String field,
        String value,
        String message,
        String suggestion
    ) {
        return new ValidationErrorDto(row, field, value, message, "error", suggestion);
    }

    public static ValidationErrorDto warning(
        int row,
        String field,
        String value,
        String message,
        String suggestion
    ) {
        return new ValidationErrorDto(row, field, value, message, "warning", suggestion);
    }

    public static ValidationErrorDto info(
        int row,
        String field,
        String value,
        String message,
        String suggestion
    ) {
        return new ValidationErrorDto(row, field, value, message, "info", suggestion);
    }

    // File-level error (no row number)
    public static ValidationErrorDto fileError(String message, String suggestion) {
        return new ValidationErrorDto(null, null, null, message, "error", suggestion);
    }

    // Getters and setters
    public Integer getRow() {
        return row;
    }

    public void setRow(Integer row) {
        this.row = row;
    }

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getSuggestion() {
        return suggestion;
    }

    public void setSuggestion(String suggestion) {
        this.suggestion = suggestion;
    }
}
