-- ============================================================================
-- Table: assessment_datasets
-- Purpose: Links risk assessments to source datasets (data provenance)
-- ============================================================================

CREATE TABLE assessment_datasets (
    id BIGSERIAL PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES risk_assessments(id) ON DELETE CASCADE,
    dataset_id UUID NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,
    dataset_type VARCHAR(50) NOT NULL,  -- 'hydro', 'community', 'infrastructure'

    CONSTRAINT check_dataset_type CHECK (
        dataset_type IN ('hydro', 'community', 'infrastructure')
    ),
    UNIQUE(assessment_id, dataset_id)  -- Prevent duplicate dataset links
);

-- Indexes
CREATE INDEX idx_assessment_datasets_assessment_id ON assessment_datasets(assessment_id);
CREATE INDEX idx_assessment_datasets_dataset_id ON assessment_datasets(dataset_id);

-- Comments
COMMENT ON TABLE assessment_datasets IS 'Tracks data provenance - which datasets were used to compute each risk assessment';
COMMENT ON CONSTRAINT check_dataset_type ON assessment_datasets IS 'Only hydro, community, and infrastructure datasets can be linked';
