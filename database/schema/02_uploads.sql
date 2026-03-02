-- ============================================================================
-- Table: uploads
-- Purpose: File upload metadata and provenance tracking
-- ============================================================================

CREATE TABLE uploads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- File metadata
    filename VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    file_checksum VARCHAR(64) NOT NULL,  -- SHA-256
    file_type VARCHAR(50) NOT NULL,  -- 'csv', 'geojson'
    data_type VARCHAR(50) NOT NULL,  -- 'hydro', 'community', 'infrastructure', 'generic'

    -- Processing status
    status VARCHAR(50) NOT NULL DEFAULT 'pending',  -- pending, processing, completed, failed
    records_imported INTEGER,
    records_failed INTEGER,
    error_message TEXT,

    -- Provenance
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,
    deleted_at TIMESTAMP,  -- Soft delete

    -- Constraints
    CONSTRAINT check_status CHECK (status IN ('pending', 'processing', 'completed', 'failed')),
    CONSTRAINT check_data_type CHECK (data_type IN ('hydro', 'community', 'infrastructure', 'generic'))
);

-- Indexes
CREATE INDEX idx_uploads_user_id ON uploads(user_id);
CREATE INDEX idx_uploads_status ON uploads(status);
CREATE INDEX idx_uploads_uploaded_at ON uploads(uploaded_at DESC);
CREATE INDEX idx_uploads_deleted_at ON uploads(deleted_at) WHERE deleted_at IS NULL;

-- Comments
COMMENT ON TABLE uploads IS 'File upload metadata and processing status';
COMMENT ON COLUMN uploads.file_checksum IS 'SHA-256 hash to prevent duplicate uploads';
COMMENT ON COLUMN uploads.status IS 'Tracks processing pipeline state';
COMMENT ON COLUMN uploads.deleted_at IS 'Soft delete preserves provenance for 30 days';
COMMENT ON COLUMN uploads.records_failed IS 'Non-zero indicates partial import (check error logs)';
