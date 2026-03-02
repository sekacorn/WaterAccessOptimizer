-- ============================================================================
-- Table: hydro_data
-- Purpose: Hydrological measurements (water quality, levels, flow)
-- ============================================================================

CREATE TABLE hydro_data (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID REFERENCES uploads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,

    -- Source and location
    source VARCHAR(100) NOT NULL,  -- "USGS", "Field Survey", "OpenStreetMap"
    location_name VARCHAR(200),
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,  -- PostGIS geography type (lat/lon)

    -- Measurement
    data_type VARCHAR(50),  -- water_quality, aquifer_level, stream_flow, precipitation
    parameter_name VARCHAR(100),  -- arsenic, pH, TDS, depth_to_water
    measurement_value NUMERIC(15,4) NOT NULL,
    measurement_unit VARCHAR(50) NOT NULL,
    measurement_date TIMESTAMP NOT NULL,

    -- Additional attributes
    depth_meters NUMERIC(10,2),
    notes TEXT,
    metadata JSONB,  -- Flexible storage for variable attributes

    -- External source tracking
    external_source_id VARCHAR(255),  -- USGS site_no, OSM node ID
    data_version VARCHAR(50),

    -- Provenance
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT check_measurement CHECK (measurement_value IS NOT NULL)
);

-- Spatial index for geography queries
CREATE INDEX idx_hydro_coordinates ON hydro_data USING GIST(coordinates);

-- Regular indexes
CREATE INDEX idx_hydro_upload_id ON hydro_data(upload_id);
CREATE INDEX idx_hydro_user_id ON hydro_data(user_id);
CREATE INDEX idx_hydro_source ON hydro_data(source);
CREATE INDEX idx_hydro_data_type ON hydro_data(data_type);
CREATE INDEX idx_hydro_measurement_date ON hydro_data(measurement_date DESC);
CREATE INDEX idx_hydro_external_id ON hydro_data(external_source_id) WHERE external_source_id IS NOT NULL;

-- Comments
COMMENT ON TABLE hydro_data IS 'Hydrological measurements (water quality, groundwater levels, stream flow)';
COMMENT ON COLUMN hydro_data.coordinates IS 'PostGIS GEOGRAPHY type - spherical earth model, accurate global distances (SRID 4326 = WGS84)';
COMMENT ON COLUMN hydro_data.metadata IS 'JSONB for flexible attributes (e.g., {"sample_depth": 10, "lab_id": "LAB-123"})';
COMMENT ON COLUMN hydro_data.external_source_id IS 'Enables upsert logic for external data refreshes';
