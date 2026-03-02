-- Create infrastructure_data table
-- Stores water facility locations and operational status

CREATE TABLE infrastructure_data (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,
    facility_id VARCHAR(100),
    facility_name VARCHAR(255) NOT NULL,
    facility_type VARCHAR(100) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    operational_status VARCHAR(50) NOT NULL,
    capacity_liters_per_day NUMERIC(12, 2),
    installation_year INTEGER,
    last_maintenance_date DATE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_operational_status CHECK (operational_status IN (
        'OPERATIONAL',
        'PARTIALLY_OPERATIONAL',
        'NON_OPERATIONAL',
        'UNDER_CONSTRUCTION',
        'ABANDONED'
    )),
    CONSTRAINT chk_capacity CHECK (capacity_liters_per_day IS NULL OR capacity_liters_per_day > 0),
    CONSTRAINT chk_installation_year CHECK (installation_year IS NULL OR (installation_year >= 1900 AND installation_year <= 2100))
);

-- Spatial index for fast geographic queries
CREATE INDEX idx_infrastructure_data_coordinates ON infrastructure_data USING GIST (coordinates);

-- Other indexes
CREATE INDEX idx_infrastructure_data_upload_id ON infrastructure_data(upload_id);
CREATE INDEX idx_infrastructure_data_facility_type ON infrastructure_data(facility_type);
CREATE INDEX idx_infrastructure_data_operational_status ON infrastructure_data(operational_status);
CREATE INDEX idx_infrastructure_data_facility_id ON infrastructure_data(facility_id);

-- Comments for documentation
COMMENT ON TABLE infrastructure_data IS 'Water facility infrastructure with operational status';
COMMENT ON COLUMN infrastructure_data.coordinates IS 'Geographic point in WGS84 (SRID 4326)';
COMMENT ON COLUMN infrastructure_data.operational_status IS 'Current operational status of the facility';
COMMENT ON COLUMN infrastructure_data.capacity_liters_per_day IS 'Daily water production capacity';
