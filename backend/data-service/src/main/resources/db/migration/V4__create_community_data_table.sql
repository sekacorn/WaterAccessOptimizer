-- Create community_data table
-- Stores community demographic and water access information

CREATE TABLE community_data (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,
    community_name VARCHAR(255) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    population INTEGER,
    households INTEGER,
    water_access_level VARCHAR(50),
    primary_water_source VARCHAR(100),
    distance_to_water_m NUMERIC(10, 2),
    service_level VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_population CHECK (population IS NULL OR population > 0),
    CONSTRAINT chk_households CHECK (households IS NULL OR households > 0),
    CONSTRAINT chk_distance CHECK (distance_to_water_m IS NULL OR distance_to_water_m >= 0),
    CONSTRAINT chk_service_level CHECK (service_level IS NULL OR service_level IN (
        'NO_ACCESS',
        'SURFACE_WATER',
        'UNIMPROVED',
        'LIMITED',
        'BASIC',
        'SAFELY_MANAGED'
    ))
);

-- Spatial index for fast geographic queries
CREATE INDEX idx_community_data_coordinates ON community_data USING GIST (coordinates);

-- Other indexes
CREATE INDEX idx_community_data_upload_id ON community_data(upload_id);
CREATE INDEX idx_community_data_population ON community_data(population DESC);
CREATE INDEX idx_community_data_service_level ON community_data(service_level);
CREATE INDEX idx_community_data_community_name ON community_data(community_name);

-- Comments for documentation
COMMENT ON TABLE community_data IS 'Community demographic and water access information';
COMMENT ON COLUMN community_data.coordinates IS 'Geographic point in WGS84 (SRID 4326)';
COMMENT ON COLUMN community_data.service_level IS 'WHO JMP Service Ladder classification';
COMMENT ON COLUMN community_data.distance_to_water_m IS 'Distance to nearest water source in meters';
