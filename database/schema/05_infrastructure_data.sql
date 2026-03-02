-- ============================================================================
-- Table: infrastructure_data
-- Purpose: Water facilities and infrastructure
-- ============================================================================

CREATE TABLE infrastructure_data (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID REFERENCES uploads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Facility info
    facility_type VARCHAR(100) NOT NULL,  -- well, borehole, treatment_plant, reservoir, etc.
    facility_name VARCHAR(200) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,

    -- Operational status
    operational_status VARCHAR(50) NOT NULL,  -- operational, non_operational, under_maintenance, planned, abandoned

    -- Capacity
    capacity NUMERIC(15,2),
    capacity_unit VARCHAR(50),  -- liters_per_day, liters_per_hour, liters, cubic_meters
    population_served INTEGER,

    -- Dates
    installation_date DATE,
    last_maintenance_date DATE,

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
    CONSTRAINT check_facility_type CHECK (
        facility_type IN ('well', 'borehole', 'treatment_plant', 'reservoir',
                          'distribution_point', 'pump_station', 'water_tower',
                          'spring_protection', 'other')
    ),
    CONSTRAINT check_operational_status CHECK (
        operational_status IN ('operational', 'non_operational', 'under_maintenance',
                               'planned', 'abandoned')
    )
);

-- Spatial index
CREATE INDEX idx_infrastructure_coordinates ON infrastructure_data USING GIST(coordinates);

-- Regular indexes
CREATE INDEX idx_infrastructure_upload_id ON infrastructure_data(upload_id);
CREATE INDEX idx_infrastructure_user_id ON infrastructure_data(user_id);
CREATE INDEX idx_infrastructure_type ON infrastructure_data(facility_type);
CREATE INDEX idx_infrastructure_status ON infrastructure_data(operational_status);

-- Comments
COMMENT ON TABLE infrastructure_data IS 'Water facilities and infrastructure with operational status';
COMMENT ON COLUMN infrastructure_data.operational_status IS 'Facility operational state affects risk scoring';
COMMENT ON COLUMN infrastructure_data.capacity IS 'Use normalize_capacity() function for unit conversion';
