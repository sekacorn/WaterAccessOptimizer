-- ============================================================================
-- Table: risk_results
-- Purpose: Individual community risk scores and explanations
-- ============================================================================

CREATE TABLE risk_results (
    id BIGSERIAL PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES risk_assessments(id) ON DELETE CASCADE,
    community_id BIGINT NOT NULL,  -- References community_data(id), but not FK (may be external)

    -- Risk score components
    risk_score INTEGER NOT NULL CHECK (risk_score >= 0 AND risk_score <= 100),
    risk_level VARCHAR(20) NOT NULL,  -- HIGH (67-100), MEDIUM (34-66), LOW (0-33)

    -- Component scores (4 factors from product requirements)
    water_quality_score INTEGER,      -- 35% weight
    access_distance_score INTEGER,    -- 30% weight
    infrastructure_score INTEGER,     -- 25% weight
    population_pressure_score INTEGER,  -- 10% weight

    -- Data confidence
    confidence_level VARCHAR(20) NOT NULL,  -- HIGH (>30 samples), MEDIUM (10-30), LOW (1-9), NONE (0)
    sample_count INTEGER NOT NULL,

    -- Top 3 contributing factors (explainability requirement EXP-1)
    explanation_json JSONB NOT NULL,

    -- Provenance
    calculated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT check_risk_level CHECK (
        risk_level IN ('HIGH', 'MEDIUM', 'LOW')
    ),
    CONSTRAINT check_confidence CHECK (
        confidence_level IN ('HIGH', 'MEDIUM', 'LOW', 'NONE')
    )
);

-- Indexes
CREATE INDEX idx_risk_results_assessment_id ON risk_results(assessment_id);
CREATE INDEX idx_risk_results_community_id ON risk_results(community_id);
CREATE INDEX idx_risk_results_risk_level ON risk_results(risk_level);
CREATE INDEX idx_risk_results_confidence ON risk_results(confidence_level);

-- GIN index for querying explanation factors
CREATE INDEX idx_risk_results_explanation ON risk_results USING GIN(explanation_json);

-- Comments
COMMENT ON TABLE risk_results IS 'Individual community risk scores with component breakdowns and explanations';
COMMENT ON COLUMN risk_results.risk_score IS 'Overall risk score (0-100, normalized)';
COMMENT ON COLUMN risk_results.water_quality_score IS 'Component score: 35% weight, measures contamination levels';
COMMENT ON COLUMN risk_results.access_distance_score IS 'Component score: 30% weight, measures distance to nearest water source';
COMMENT ON COLUMN risk_results.infrastructure_score IS 'Component score: 25% weight, measures facility reliability';
COMMENT ON COLUMN risk_results.population_pressure_score IS 'Component score: 10% weight, measures population vs capacity';
COMMENT ON COLUMN risk_results.explanation_json IS 'Top 3 contributing factors with measured values, guidelines, and impact descriptions (EXP-1 requirement)';
