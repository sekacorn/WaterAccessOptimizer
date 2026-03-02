-- ============================================================================
-- Table: risk_assessments
-- Purpose: Risk assessment metadata and provenance
-- ============================================================================
-- Design Note: Risk assessment data is split into 3 normalized tables:
--   1. risk_assessments (metadata)
--   2. assessment_datasets (links assessments to source datasets)
--   3. risk_results (individual community risk scores)
-- ============================================================================

CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Analysis metadata
    name VARCHAR(255) NOT NULL DEFAULT 'Risk Assessment',
    description TEXT,
    algorithm_version VARCHAR(20) NOT NULL,  -- e.g., "1.0.0" for reproducibility

    -- Processing metadata
    calculation_duration_ms INTEGER,
    total_communities_analyzed INTEGER,

    -- Provenance
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,  -- Optional expiration for temporary analyses

    -- Visibility (v1)
    is_public BOOLEAN NOT NULL DEFAULT false,
    shared_with_user_ids UUID[]  -- Array of user IDs (v1 feature)
);

-- Indexes
CREATE INDEX idx_risk_user_id ON risk_assessments(user_id);
CREATE INDEX idx_risk_created_at ON risk_assessments(created_at DESC);
CREATE INDEX idx_risk_public ON risk_assessments(is_public) WHERE is_public = true;

-- Comments
COMMENT ON TABLE risk_assessments IS 'Risk assessment metadata and provenance tracking';
COMMENT ON COLUMN risk_assessments.algorithm_version IS 'Tracks which version of risk scoring algorithm was used (enables reproducibility)';
COMMENT ON COLUMN risk_assessments.expires_at IS 'Optional for temporary/preview assessments (cleaned up by cron job)';
