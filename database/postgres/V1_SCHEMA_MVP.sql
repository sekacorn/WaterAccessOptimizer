-- WaterAccessOptimizer PostgreSQL Schema - MVP v0.1.0
-- Based on Agent 02 (Data Sources) + Agent 03 (Domain Model/DB Schema)
--
-- This schema supports MVP ONLY (Weeks 1-12):
-- - Manual CSV/GeoJSON upload
-- - Rule-based risk assessment
-- - 2 user roles (USER, ADMIN)
-- - Basic audit logging
-- - Refresh tokens (Sprint 2)
--
-- V2 features removed: MBTI, MFA, SSO, Enterprises, Moderation, 3D visualization
-- V1 features removed: LLM service, real-time collaboration, external API connectors

-- =============================================================================
-- EXTENSIONS
-- =============================================================================

CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";  -- Required for spatial queries

-- =============================================================================
-- SCHEMAS (separate schemas per service for logical isolation)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS auth_schema;
CREATE SCHEMA IF NOT EXISTS data_schema;
CREATE SCHEMA IF NOT EXISTS analysis_schema;

-- =============================================================================
-- AUTH SCHEMA - Authentication and User Management
-- =============================================================================

-- 1. users table
CREATE TABLE IF NOT EXISTS auth_schema.users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,  -- bcrypt hash with cost factor 12
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    organization VARCHAR(200),

    -- Role (MVP: USER, ADMIN only)
    role VARCHAR(50) NOT NULL DEFAULT 'USER',

    -- Storage quota (MVP: 100MB per user)
    storage_quota_mb INTEGER NOT NULL DEFAULT 100,
    storage_used_mb NUMERIC(10,2) NOT NULL DEFAULT 0,

    -- Account status
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_email_verified BOOLEAN NOT NULL DEFAULT false,

    -- Security: Account lockout (Sprint 3)
    failed_login_attempts INTEGER NOT NULL DEFAULT 0,
    locked_until TIMESTAMP,

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP,

    -- Constraints
    CONSTRAINT check_role CHECK (role IN ('USER', 'ADMIN')),
    CONSTRAINT check_storage CHECK (storage_used_mb <= storage_quota_mb)
);

CREATE INDEX idx_users_email ON auth_schema.users(email);
CREATE INDEX idx_users_role ON auth_schema.users(role);
CREATE INDEX idx_users_active ON auth_schema.users(is_active);

-- 2. refresh_tokens table (Sprint 2 - Iteration 3 security work)
CREATE TABLE IF NOT EXISTS auth_schema.refresh_tokens (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,

    -- Token identification
    token_jti VARCHAR(255) UNIQUE NOT NULL,  -- JWT ID from token
    expires_at TIMESTAMP NOT NULL,
    revoked_at TIMESTAMP,

    -- Device fingerprinting
    ip_address INET,
    user_agent TEXT,
    device_fingerprint VARCHAR(64),

    -- Timestamps
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT check_not_expired CHECK (revoked_at IS NULL OR revoked_at <= NOW())
);

CREATE INDEX idx_refresh_tokens_user_id ON auth_schema.refresh_tokens(user_id);
CREATE INDEX idx_refresh_tokens_jti ON auth_schema.refresh_tokens(token_jti);
CREATE INDEX idx_refresh_tokens_expires_at ON auth_schema.refresh_tokens(expires_at);

-- 3. audit_logs table (Sprint 3 - database-backed audit logging)
CREATE TABLE IF NOT EXISTS auth_schema.audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth_schema.users(id) ON DELETE SET NULL,

    -- Event details
    event_type VARCHAR(100) NOT NULL,  -- login_success, login_failed, data_uploaded, etc.
    event_category VARCHAR(50) NOT NULL,  -- auth, data, admin, api, security
    resource_type VARCHAR(50),  -- user, upload, assessment
    resource_id VARCHAR(255),

    -- Request context
    ip_address INET,
    user_agent TEXT,
    http_method VARCHAR(10),
    endpoint VARCHAR(255),
    status_code INTEGER,

    -- Additional details (JSONB for structured data)
    details JSONB,

    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT check_event_category CHECK (
        event_category IN ('auth', 'data', 'admin', 'api', 'security')
    )
);

CREATE INDEX idx_audit_user_id ON auth_schema.audit_logs(user_id);
CREATE INDEX idx_audit_event_type ON auth_schema.audit_logs(event_type);
CREATE INDEX idx_audit_category ON auth_schema.audit_logs(event_category);
CREATE INDEX idx_audit_created_at ON auth_schema.audit_logs(created_at DESC);
CREATE INDEX idx_audit_resource ON auth_schema.audit_logs(resource_type, resource_id);

-- =============================================================================
-- DATA SCHEMA - Data Upload and Storage
-- =============================================================================

-- 4. uploads table (file metadata and provenance)
CREATE TABLE IF NOT EXISTS data_schema.uploads (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,

    -- File metadata
    filename VARCHAR(255) NOT NULL,
    file_size_bytes BIGINT NOT NULL,
    file_checksum VARCHAR(64) NOT NULL,  -- SHA-256 for deduplication
    file_type VARCHAR(50) NOT NULL,  -- 'csv', 'geojson'
    data_type VARCHAR(50) NOT NULL,  -- 'hydro', 'community', 'infrastructure'

    -- Processing status
    status VARCHAR(50) NOT NULL DEFAULT 'pending',  -- pending, processing, completed, failed
    records_imported INTEGER,
    records_failed INTEGER,
    error_message TEXT,

    -- Provenance
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    processed_at TIMESTAMP,
    deleted_at TIMESTAMP,  -- Soft delete (retain for 30 days)

    -- Constraints
    CONSTRAINT check_status CHECK (status IN ('pending', 'processing', 'completed', 'failed')),
    CONSTRAINT check_data_type CHECK (data_type IN ('hydro', 'community', 'infrastructure'))
);

CREATE INDEX idx_uploads_user_id ON data_schema.uploads(user_id);
CREATE INDEX idx_uploads_status ON data_schema.uploads(status);
CREATE INDEX idx_uploads_uploaded_at ON data_schema.uploads(uploaded_at DESC);
CREATE INDEX idx_uploads_deleted_at ON data_schema.uploads(deleted_at) WHERE deleted_at IS NULL;
CREATE INDEX idx_uploads_checksum ON data_schema.uploads(file_checksum);

-- 5. hydro_data table (hydrological measurements)
CREATE TABLE IF NOT EXISTS data_schema.hydro_data (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID REFERENCES data_schema.uploads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,

    -- Source and location
    source VARCHAR(100) NOT NULL,  -- "USGS", "Field Survey", "WHO"
    location_name VARCHAR(200),
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,  -- PostGIS geography (lat/lon)

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

    -- External source tracking (V1 feature for API connectors)
    external_source_id VARCHAR(255),
    data_version VARCHAR(50),

    -- Provenance
    uploaded_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_updated_at TIMESTAMP NOT NULL DEFAULT NOW(),

    CONSTRAINT check_measurement CHECK (measurement_value IS NOT NULL)
);

-- Spatial index for geography queries (REQUIRED for PostGIS performance)
CREATE INDEX idx_hydro_coordinates ON data_schema.hydro_data USING GIST(coordinates);

-- Regular indexes
CREATE INDEX idx_hydro_upload_id ON data_schema.hydro_data(upload_id);
CREATE INDEX idx_hydro_user_id ON data_schema.hydro_data(user_id);
CREATE INDEX idx_hydro_source ON data_schema.hydro_data(source);
CREATE INDEX idx_hydro_data_type ON data_schema.hydro_data(data_type);
CREATE INDEX idx_hydro_measurement_date ON data_schema.hydro_data(measurement_date DESC);
CREATE INDEX idx_hydro_external_id ON data_schema.hydro_data(external_source_id) WHERE external_source_id IS NOT NULL;

-- 6. community_data table (population and water access)
CREATE TABLE IF NOT EXISTS data_schema.community_data (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID REFERENCES data_schema.uploads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,

    -- Community info
    community_name VARCHAR(200) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    population INTEGER NOT NULL CHECK (population > 0),
    household_count INTEGER,

    -- Water access (WHO JMP Service Ladder)
    water_access_level VARCHAR(50),  -- none, limited, basic, safely_managed
    primary_water_source VARCHAR(100),  -- well, borehole, piped_water, surface_water, rainwater, vendor
    collection_date DATE,

    -- Additional attributes
    notes TEXT,
    metadata JSONB,

    -- External source tracking (V1)
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

CREATE INDEX idx_community_coordinates ON data_schema.community_data USING GIST(coordinates);
CREATE INDEX idx_community_upload_id ON data_schema.community_data(upload_id);
CREATE INDEX idx_community_user_id ON data_schema.community_data(user_id);
CREATE INDEX idx_community_access_level ON data_schema.community_data(water_access_level);

-- 7. infrastructure_data table (water facilities)
CREATE TABLE IF NOT EXISTS data_schema.infrastructure_data (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID REFERENCES data_schema.uploads(id) ON DELETE CASCADE,
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,

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

    -- External source tracking (V1)
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

CREATE INDEX idx_infrastructure_coordinates ON data_schema.infrastructure_data USING GIST(coordinates);
CREATE INDEX idx_infrastructure_upload_id ON data_schema.infrastructure_data(upload_id);
CREATE INDEX idx_infrastructure_user_id ON data_schema.infrastructure_data(user_id);
CREATE INDEX idx_infrastructure_type ON data_schema.infrastructure_data(facility_type);
CREATE INDEX idx_infrastructure_status ON data_schema.infrastructure_data(operational_status);

-- 8. data_validation_errors table (upload troubleshooting)
CREATE TABLE IF NOT EXISTS data_schema.data_validation_errors (
    id BIGSERIAL PRIMARY KEY,
    upload_id UUID NOT NULL REFERENCES data_schema.uploads(id) ON DELETE CASCADE,

    -- Error details
    row_number INTEGER,
    column_name VARCHAR(100),
    error_type VARCHAR(100) NOT NULL,
    error_message TEXT NOT NULL,
    provided_value TEXT,

    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_validation_upload_id ON data_schema.data_validation_errors(upload_id);
CREATE INDEX idx_validation_error_type ON data_schema.data_validation_errors(error_type);

-- =============================================================================
-- ANALYSIS SCHEMA - Risk Assessments and Results
-- =============================================================================

-- 9. risk_assessments table (assessment metadata)
CREATE TABLE IF NOT EXISTS analysis_schema.risk_assessments (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID NOT NULL REFERENCES auth_schema.users(id) ON DELETE CASCADE,

    -- Analysis metadata
    name VARCHAR(255) NOT NULL DEFAULT 'Risk Assessment',
    description TEXT,
    algorithm_version VARCHAR(20) NOT NULL,  -- e.g., "1.0.0" for reproducibility

    -- Processing metadata
    calculation_duration_ms INTEGER,
    total_communities_analyzed INTEGER,

    -- Provenance
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP,  -- Optional expiration for temporary analyses

    -- Visibility (V1 feature)
    is_public BOOLEAN NOT NULL DEFAULT false
);

CREATE INDEX idx_risk_user_id ON analysis_schema.risk_assessments(user_id);
CREATE INDEX idx_risk_created_at ON analysis_schema.risk_assessments(created_at DESC);
CREATE INDEX idx_risk_public ON analysis_schema.risk_assessments(is_public) WHERE is_public = true;

-- 10. assessment_datasets table (data provenance linking)
CREATE TABLE IF NOT EXISTS analysis_schema.assessment_datasets (
    id BIGSERIAL PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES analysis_schema.risk_assessments(id) ON DELETE CASCADE,
    dataset_id UUID NOT NULL REFERENCES data_schema.uploads(id) ON DELETE CASCADE,
    dataset_type VARCHAR(50) NOT NULL,  -- 'hydro', 'community', 'infrastructure'

    CONSTRAINT check_dataset_type CHECK (
        dataset_type IN ('hydro', 'community', 'infrastructure')
    ),
    UNIQUE(assessment_id, dataset_id)  -- Prevent duplicate dataset links
);

CREATE INDEX idx_assessment_datasets_assessment_id ON analysis_schema.assessment_datasets(assessment_id);
CREATE INDEX idx_assessment_datasets_dataset_id ON analysis_schema.assessment_datasets(dataset_id);

-- 11. risk_results table (individual community risk scores)
CREATE TABLE IF NOT EXISTS analysis_schema.risk_results (
    id BIGSERIAL PRIMARY KEY,
    assessment_id UUID NOT NULL REFERENCES analysis_schema.risk_assessments(id) ON DELETE CASCADE,
    community_id BIGINT NOT NULL,  -- References community_data(id), but not FK (may be external)

    -- Risk score components
    risk_score INTEGER NOT NULL CHECK (risk_score >= 0 AND risk_score <= 100),
    risk_level VARCHAR(20) NOT NULL,  -- HIGH (67-100), MEDIUM (34-66), LOW (0-33)

    -- Component scores (4 factors from Agent 01 product requirements)
    water_quality_score INTEGER,      -- 35% weight
    access_distance_score INTEGER,    -- 30% weight
    infrastructure_score INTEGER,     -- 25% weight
    population_pressure_score INTEGER,  -- 10% weight

    -- Data confidence
    confidence_level VARCHAR(20) NOT NULL,  -- HIGH (>30 samples), MEDIUM (10-30), LOW (1-9), NONE (0)
    sample_count INTEGER NOT NULL,

    -- Top 3 contributing factors (explainability requirement EXP-1 from Agent 01)
    -- Structure: {top_factors: [{factor, weight, measured_value, guideline_value, impact, ...}]}
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

CREATE INDEX idx_risk_results_assessment_id ON analysis_schema.risk_results(assessment_id);
CREATE INDEX idx_risk_results_community_id ON analysis_schema.risk_results(community_id);
CREATE INDEX idx_risk_results_risk_level ON analysis_schema.risk_results(risk_level);
CREATE INDEX idx_risk_results_confidence ON analysis_schema.risk_results(confidence_level);

-- GIN index for querying explanation factors (JSONB queries)
CREATE INDEX idx_risk_results_explanation ON analysis_schema.risk_results USING GIN(explanation_json);

-- =============================================================================
-- FUNCTIONS AND TRIGGERS
-- =============================================================================

-- Auto-update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at triggers
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON auth_schema.users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Capacity normalization function (normalize all to liters_per_day)
CREATE OR REPLACE FUNCTION data_schema.normalize_capacity(cap NUMERIC, unit VARCHAR)
RETURNS NUMERIC AS $$
BEGIN
    RETURN CASE unit
        WHEN 'liters_per_day' THEN cap
        WHEN 'liters_per_hour' THEN cap * 24
        WHEN 'cubic_meters' THEN cap * 1000  -- assuming daily for storage
        WHEN 'liters' THEN cap  -- storage capacity
        ELSE NULL
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- =============================================================================
-- INITIAL DATA (MVP Admin User)
-- =============================================================================

-- Insert default admin user (password: ChangeMe123!)
-- Password hash generated with bcrypt cost factor 12
-- IMPORTANT: Change password immediately after first login
INSERT INTO auth_schema.users (
    email,
    password_hash,
    first_name,
    last_name,
    organization,
    role,
    is_active,
    is_email_verified,
    storage_quota_mb
) VALUES (
    'admin@wateroptimizer.local',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TupxIFm5K4Y8Dg9qG7wI1j0wD.1C',  -- ChangeMe123!
    'System',
    'Administrator',
    'WaterAccessOptimizer',
    'ADMIN',
    true,
    true,
    1000  -- 1GB for admin
) ON CONFLICT (email) DO NOTHING;

-- =============================================================================
-- COMMENTS (Documentation)
-- =============================================================================

COMMENT ON SCHEMA auth_schema IS 'Authentication and user management (Auth Service)';
COMMENT ON SCHEMA data_schema IS 'Data upload and storage (Data Service)';
COMMENT ON SCHEMA analysis_schema IS 'Risk assessments and results (Worker Service)';

COMMENT ON TABLE auth_schema.users IS 'User accounts with USER/ADMIN roles (MVP)';
COMMENT ON TABLE auth_schema.refresh_tokens IS 'Refresh tokens for JWT rotation (Sprint 2)';
COMMENT ON TABLE auth_schema.audit_logs IS 'Security and compliance audit trail (Sprint 3)';

COMMENT ON TABLE data_schema.uploads IS 'File upload metadata and provenance tracking';
COMMENT ON TABLE data_schema.hydro_data IS 'Hydrological measurements (water quality, aquifer levels)';
COMMENT ON TABLE data_schema.community_data IS 'Population and water access data (WHO JMP levels)';
COMMENT ON TABLE data_schema.infrastructure_data IS 'Water facilities and infrastructure';
COMMENT ON TABLE data_schema.data_validation_errors IS 'Upload validation errors for troubleshooting';

COMMENT ON TABLE analysis_schema.risk_assessments IS 'Risk assessment metadata and provenance';
COMMENT ON TABLE analysis_schema.assessment_datasets IS 'Links assessments to source datasets (provenance)';
COMMENT ON TABLE analysis_schema.risk_results IS 'Individual community risk scores with explainability';

-- =============================================================================
-- VERIFICATION QUERIES
-- =============================================================================

-- Verify PostGIS extension
SELECT PostGIS_Full_Version();

-- Verify schemas
SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE '%_schema' ORDER BY schema_name;

-- Verify tables
SELECT
    schemaname,
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname LIKE '%_schema'
ORDER BY schemaname, tablename;

-- Verify spatial indexes
SELECT
    schemaname,
    tablename,
    indexname,
    indexdef
FROM pg_indexes
WHERE indexname LIKE '%coordinates%'
ORDER BY schemaname, tablename;
