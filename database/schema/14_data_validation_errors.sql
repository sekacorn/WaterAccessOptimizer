-- ============================================================================
-- Table: data_validation_errors
-- Purpose: Store validation errors for troubleshooting data uploads
-- ============================================================================

CREATE TABLE data_validation_errors (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,

    -- Error details
    row_number INTEGER,
    column_name VARCHAR(100),
    error_type VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    provided_value TEXT,

    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_validation_upload_id ON data_validation_errors(upload_id);
CREATE INDEX idx_validation_error_type ON data_validation_errors(error_type);

-- Comments
COMMENT ON TABLE data_validation_errors IS 'Validation errors for troubleshooting. User sees summary in API response, full details available via GET endpoint.';
COMMENT ON COLUMN data_validation_errors.row_number IS 'CSV row number (1-indexed, excludes header)';
COMMENT ON COLUMN data_validation_errors.error_type IS 'Error type: missing_required, invalid_format, out_of_range, duplicate, invalid_coordinates, etc.';
