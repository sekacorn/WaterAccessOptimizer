-- ============================================================================
-- Table: projects
-- Purpose: User-created analysis projects (V1 feature)
-- ============================================================================

CREATE TABLE projects (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Project info
    name VARCHAR(200) NOT NULL,
    description TEXT,

    -- Contents
    upload_ids UUID[] NOT NULL,  -- Datasets in this project
    risk_assessment_ids UUID[],  -- Analyses in this project

    -- Sharing (v1)
    is_public BOOLEAN NOT NULL DEFAULT false,
    collaborator_ids UUID[],  -- Users with edit access
    viewer_ids UUID[],  -- Users with read-only access

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_accessed_at TIMESTAMP
);

-- Indexes
CREATE INDEX idx_projects_user_id ON projects(user_id);
CREATE INDEX idx_projects_updated_at ON projects(updated_at DESC);

-- Comments
COMMENT ON TABLE projects IS 'User-created analysis projects for organizing datasets and assessments (V1 feature)';
COMMENT ON COLUMN projects.upload_ids IS 'Array of dataset UUIDs included in this project';
COMMENT ON COLUMN projects.risk_assessment_ids IS 'Array of risk assessment UUIDs in this project';
