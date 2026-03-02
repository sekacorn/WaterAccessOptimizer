-- ============================================================================
-- Table: community_data
-- Purpose: Population and water access data for communities
-- ============================================================================

CREATE TABLE community_data (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID REFERENCES uploads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Community info
    community_name VARCHAR(200) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    population INTEGER NOT NULL CHECK (population > 0),
    household_count INTEGER,

    -- Water access
    water_access_level VARCHAR(50),  -- none, limited, basic, safely_managed (WHO JMP)
    primary_water_source VARCHAR(100),  -- well, borehole, piped_water, surface_water, rainwater, vendor
    collection_date DATE,

    -- Additional attributes
    notes TEXT,
    metadata JSONB,

    -- External source tracking
    external_source_id VARCHAR(255),
    source VARCHAR(100) NOT NULL,

    -- Provenance
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT check_access_level CHECK (
        water_access_level IS NULL OR
        water_access_level IN ('none', 'limited', 'basic', 'safely_managed')
    )
);

-- Spatial index
CREATE INDEX idx_community_coordinates ON community_data USING GIST(coordinates);

-- Regular indexes
CREATE INDEX idx_community_upload_id ON community_data(upload_id);
CREATE INDEX idx_community_user_id ON community_data(user_id);
CREATE INDEX idx_community_access_level ON community_data(water_access_level);

-- Comments
COMMENT ON TABLE community_data IS 'Population centers with water access information';
COMMENT ON COLUMN community_data.water_access_level IS 'WHO JMP Service Ladder: safely_managed, basic, limited, none';
COMMENT ON COLUMN community_data.coordinates IS 'Community center coordinates (GEOGRAPHY POINT, SRID 4326)';
