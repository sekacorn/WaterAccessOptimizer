-- Additional composite indexes for query optimization
-- These indexes improve performance for common query patterns

-- Users: Active users with available quota
CREATE INDEX idx_users_active_with_quota ON users(is_active, storage_used_mb)
    WHERE is_active = true;

-- Uploads: Non-deleted uploads by user and type
CREATE INDEX idx_uploads_user_type_active ON uploads(user_id, data_type, uploaded_at DESC)
    WHERE deleted_at IS NULL;

-- Uploads: Validation status filtering
CREATE INDEX idx_uploads_status_active ON uploads(validation_status, uploaded_at DESC)
    WHERE deleted_at IS NULL;

-- Hydro Data: Spatial queries with quality parameters
CREATE INDEX idx_hydro_data_quality_checks ON hydro_data(arsenic_mg_l, fluoride_mg_l, nitrate_mg_l, ecoli_cfu_100ml)
    WHERE arsenic_mg_l IS NOT NULL OR fluoride_mg_l IS NOT NULL OR nitrate_mg_l IS NOT NULL OR ecoli_cfu_100ml IS NOT NULL;

-- Community Data: Population density queries
CREATE INDEX idx_community_data_population_service ON community_data(population, service_level)
    WHERE population IS NOT NULL;

-- Infrastructure Data: Operational facilities only
CREATE INDEX idx_infrastructure_operational ON infrastructure_data(operational_status, facility_type)
    WHERE operational_status = 'OPERATIONAL';

-- Risk Results: Assessment summary queries (count by level)
CREATE INDEX idx_risk_results_assessment_level ON risk_results(assessment_id, risk_level, risk_score DESC);

-- Risk Results: High confidence results
CREATE INDEX idx_risk_results_high_confidence ON risk_results(confidence_level, risk_score DESC)
    WHERE confidence_level IN ('HIGH', 'MEDIUM');

-- Comments for optimization notes
COMMENT ON INDEX idx_users_active_with_quota IS 'Optimize active user quota checks';
COMMENT ON INDEX idx_uploads_user_type_active IS 'Optimize user upload history queries';
COMMENT ON INDEX idx_risk_results_assessment_level IS 'Optimize assessment summary statistics';
