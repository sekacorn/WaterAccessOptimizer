-- Create risk_assessments table
-- Tracks risk assessment runs with algorithm versioning

CREATE TABLE risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL DEFAULT 'Risk Assessment',
    description TEXT,
    algorithm_version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    calculation_duration_ms INTEGER,
    total_communities_analyzed INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,
    is_public BOOLEAN NOT NULL DEFAULT false,

    CONSTRAINT chk_calculation_duration CHECK (calculation_duration_ms IS NULL OR calculation_duration_ms >= 0),
    CONSTRAINT chk_total_communities CHECK (total_communities_analyzed IS NULL OR total_communities_analyzed >= 0)
);

-- Indexes for risk_assessments table
CREATE INDEX idx_risk_assessments_user_id ON risk_assessments(user_id);
CREATE INDEX idx_risk_assessments_created_at ON risk_assessments(created_at DESC);
CREATE INDEX idx_risk_assessments_is_public ON risk_assessments(is_public) WHERE is_public = true;
CREATE INDEX idx_risk_assessments_expires_at ON risk_assessments(expires_at) WHERE expires_at IS NOT NULL;

-- Comments for documentation
COMMENT ON TABLE risk_assessments IS 'Risk assessment metadata with algorithm version tracking';
COMMENT ON COLUMN risk_assessments.algorithm_version IS 'Algorithm version for reproducibility (e.g., 1.0.0)';
COMMENT ON COLUMN risk_assessments.is_public IS 'Whether assessment is visible to all users';
COMMENT ON COLUMN risk_assessments.expires_at IS 'Expiration timestamp for automatic cleanup';
