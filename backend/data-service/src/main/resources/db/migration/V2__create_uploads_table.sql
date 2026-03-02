-- Create uploads table
-- Tracks file uploads with validation results and soft delete support

CREATE TABLE uploads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    data_type VARCHAR(50) NOT NULL,
    filename VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    file_checksum VARCHAR(64) NOT NULL,
    row_count INTEGER NOT NULL DEFAULT 0,
    validation_status VARCHAR(20) NOT NULL,
    error_count INTEGER NOT NULL DEFAULT 0,
    warning_count INTEGER NOT NULL DEFAULT 0,
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    deleted_at TIMESTAMP,

    CONSTRAINT chk_data_type CHECK (data_type IN ('HYDRO', 'COMMUNITY', 'INFRASTRUCTURE')),
    CONSTRAINT chk_validation_status CHECK (validation_status IN ('PASSED', 'FAILED', 'WARNING')),
    CONSTRAINT chk_file_size CHECK (file_size_bytes > 0),
    CONSTRAINT chk_row_count CHECK (row_count >= 0),
    CONSTRAINT chk_error_count CHECK (error_count >= 0),
    CONSTRAINT chk_warning_count CHECK (warning_count >= 0)
);

-- Indexes for uploads table
CREATE INDEX idx_uploads_user_id ON uploads(user_id);
CREATE INDEX idx_uploads_data_type ON uploads(data_type);
CREATE INDEX idx_uploads_uploaded_at ON uploads(uploaded_at DESC);
CREATE INDEX idx_uploads_deleted_at ON uploads(deleted_at) WHERE deleted_at IS NOT NULL;
CREATE INDEX idx_uploads_validation_status ON uploads(validation_status);

-- Comments for documentation
COMMENT ON TABLE uploads IS 'Upload metadata with validation results and soft delete support';
COMMENT ON COLUMN uploads.data_type IS 'Type of data: HYDRO, COMMUNITY, or INFRASTRUCTURE';
COMMENT ON COLUMN uploads.file_checksum IS 'SHA-256 checksum for duplicate detection';
COMMENT ON COLUMN uploads.deleted_at IS 'Soft delete timestamp (NULL if active)';
