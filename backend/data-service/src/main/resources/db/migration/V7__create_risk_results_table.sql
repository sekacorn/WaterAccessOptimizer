-- Create risk_results table
-- Stores individual community risk scores with component breakdown

CREATE TABLE risk_results (
    id BIGSERIAL PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES risk_assessments(id) ON DELETE CASCADE,
    community_id BIGINT NOT NULL,
    risk_score INTEGER NOT NULL,
    risk_level VARCHAR(20) NOT NULL,
    water_quality_score INTEGER,
    access_distance_score INTEGER,
    infrastructure_score INTEGER,
    population_pressure_score INTEGER,
    confidence_level VARCHAR(20) NOT NULL,
    sample_count INTEGER NOT NULL,
    explanation_json JSONB NOT NULL,
    calculated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_risk_score CHECK (risk_score >= 0 AND risk_score <= 100),
    CONSTRAINT chk_risk_level CHECK (risk_level IN ('HIGH', 'MEDIUM', 'LOW')),
    CONSTRAINT chk_water_quality_score CHECK (water_quality_score IS NULL OR (water_quality_score >= 0 AND water_quality_score <= 100)),
    CONSTRAINT chk_access_distance_score CHECK (access_distance_score IS NULL OR (access_distance_score >= 0 AND access_distance_score <= 100)),
    CONSTRAINT chk_infrastructure_score CHECK (infrastructure_score IS NULL OR (infrastructure_score >= 0 AND infrastructure_score <= 100)),
    CONSTRAINT chk_population_pressure_score CHECK (population_pressure_score IS NULL OR (population_pressure_score >= 0 AND population_pressure_score <= 100)),
    CONSTRAINT chk_confidence_level CHECK (confidence_level IN ('HIGH', 'MEDIUM', 'LOW', 'NONE')),
    CONSTRAINT chk_sample_count CHECK (sample_count >= 0)
);

-- Indexes for risk_results table
CREATE INDEX idx_risk_results_assessment_id ON risk_results(assessment_id);
CREATE INDEX idx_risk_results_community_id ON risk_results(community_id);
CREATE INDEX idx_risk_results_risk_score ON risk_results(risk_score DESC);
CREATE INDEX idx_risk_results_risk_level ON risk_results(risk_level);
CREATE INDEX idx_risk_results_calculated_at ON risk_results(calculated_at DESC);

-- GIN index for JSONB explanation field (for querying explanation factors)
CREATE INDEX idx_risk_results_explanation_json ON risk_results USING GIN (explanation_json);

-- Comments for documentation
COMMENT ON TABLE risk_results IS 'Individual community risk scores with component breakdown';
COMMENT ON COLUMN risk_results.risk_score IS 'Overall risk score (0-100)';
COMMENT ON COLUMN risk_results.risk_level IS 'Risk classification: HIGH (67-100), MEDIUM (34-66), LOW (0-33)';
COMMENT ON COLUMN risk_results.water_quality_score IS 'Water quality component (35% weight)';
COMMENT ON COLUMN risk_results.access_distance_score IS 'Access distance component (30% weight)';
COMMENT ON COLUMN risk_results.infrastructure_score IS 'Infrastructure component (25% weight)';
COMMENT ON COLUMN risk_results.population_pressure_score IS 'Population pressure component (10% weight)';
COMMENT ON COLUMN risk_results.explanation_json IS 'Top 3 contributing factors in JSON format';
COMMENT ON COLUMN risk_results.confidence_level IS 'Data quality: HIGH (>30 samples), MEDIUM (10-30), LOW (1-9), NONE (0)';
