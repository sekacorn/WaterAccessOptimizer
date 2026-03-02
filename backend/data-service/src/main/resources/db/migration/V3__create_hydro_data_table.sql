-- Enable PostGIS extension for spatial data support
CREATE EXTENSION IF NOT EXISTS postgis;

-- Create hydro_data table
-- Stores water quality measurements with geospatial coordinates

CREATE TABLE hydro_data (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID NOT NULL REFERENCES uploads(id) ON DELETE CASCADE,
    station_id VARCHAR(100),
    station_name VARCHAR(255),
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    measurement_date DATE,
    ph NUMERIC(4, 2),
    turbidity_ntu NUMERIC(10, 2),
    tds_mg_l NUMERIC(10, 2),
    arsenic_mg_l NUMERIC(10, 5),
    fluoride_mg_l NUMERIC(10, 3),
    nitrate_mg_l NUMERIC(10, 2),
    ecoli_cfu_100ml INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT chk_ph CHECK (ph IS NULL OR (ph >= 0 AND ph <= 14)),
    CONSTRAINT chk_turbidity CHECK (turbidity_ntu IS NULL OR turbidity_ntu >= 0),
    CONSTRAINT chk_tds CHECK (tds_mg_l IS NULL OR tds_mg_l >= 0),
    CONSTRAINT chk_arsenic CHECK (arsenic_mg_l IS NULL OR arsenic_mg_l >= 0),
    CONSTRAINT chk_fluoride CHECK (fluoride_mg_l IS NULL OR fluoride_mg_l >= 0),
    CONSTRAINT chk_nitrate CHECK (nitrate_mg_l IS NULL OR nitrate_mg_l >= 0),
    CONSTRAINT chk_ecoli CHECK (ecoli_cfu_100ml IS NULL OR ecoli_cfu_100ml >= 0)
);

-- Spatial index for fast geographic queries
CREATE INDEX idx_hydro_data_coordinates ON hydro_data USING GIST (coordinates);

-- Other indexes
CREATE INDEX idx_hydro_data_upload_id ON hydro_data(upload_id);
CREATE INDEX idx_hydro_data_measurement_date ON hydro_data(measurement_date DESC);
CREATE INDEX idx_hydro_data_station_id ON hydro_data(station_id);

-- Comments for documentation
COMMENT ON TABLE hydro_data IS 'Water quality measurements with geospatial data (PostGIS)';
COMMENT ON COLUMN hydro_data.coordinates IS 'Geographic point in WGS84 (SRID 4326)';
COMMENT ON COLUMN hydro_data.ph IS 'pH level (0-14 scale)';
COMMENT ON COLUMN hydro_data.arsenic_mg_l IS 'Arsenic concentration (WHO limit: 0.01 mg/L)';
COMMENT ON COLUMN hydro_data.fluoride_mg_l IS 'Fluoride concentration (WHO limit: 1.5 mg/L)';
COMMENT ON COLUMN hydro_data.nitrate_mg_l IS 'Nitrate concentration (WHO limit: 50 mg/L)';
COMMENT ON COLUMN hydro_data.ecoli_cfu_100ml IS 'E. coli count per 100ml (WHO limit: 0)';
