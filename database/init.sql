-- ============================================================================
-- WaterAccessOptimizer Database Initialization Script
-- ============================================================================
-- Purpose: Master initialization script for PostgreSQL + PostGIS database
-- Author: WaterAccessOptimizer Team
-- Version: 1.0.0
-- Date: 2026-02-03
-- ============================================================================

-- Enable required PostgreSQL extensions
CREATE EXTENSION IF NOT EXISTS postgis;
CREATE EXTENSION IF NOT EXISTS pg_stat_statements;
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- ============================================================================
-- Create MVP Tables (in dependency order)
-- ============================================================================

\echo 'Creating users table...'
\i schema/01_users.sql

\echo 'Creating uploads table...'
\i schema/02_uploads.sql

\echo 'Creating hydro_data table...'
\i schema/03_hydro_data.sql

\echo 'Creating community_data table...'
\i schema/04_community_data.sql

\echo 'Creating infrastructure_data table...'
\i schema/05_infrastructure_data.sql

\echo 'Creating risk_assessments tables...'
\i schema/06a_risk_assessments.sql
\i schema/06b_assessment_datasets.sql
\i schema/06c_risk_results.sql

\echo 'Creating projects table...'
\i schema/07_projects.sql

\echo 'Creating audit_logs table...'
\i schema/12_audit_logs.sql

\echo 'Creating supporting tables...'
\i schema/13_spatial_cache.sql
\i schema/14_data_validation_errors.sql

-- ============================================================================
-- Create V1 Tables (deferred to V1 release)
-- ============================================================================

-- \echo 'Creating LLM query log table...'
-- \i schema/08_llm_query_log.sql

-- \echo 'Creating collaboration tables...'
-- \i schema/09_collaboration_sessions.sql
-- \i schema/10_session_participants.sql
-- \i schema/11_session_comments.sql

-- \echo 'Creating external sync log table...'
-- \i schema/15_external_sync_log.sql

-- ============================================================================
-- Create Triggers
-- ============================================================================

\echo 'Creating triggers...'
\i triggers/update_timestamps.sql

-- ============================================================================
-- Create Functions
-- ============================================================================

\echo 'Creating utility functions...'
\i functions/normalize_capacity.sql

-- ============================================================================
-- Load Seed Data (environment-specific)
-- ============================================================================

-- Uncomment for development environment:
-- \i seed/dev/default_users.sql
-- \i seed/dev/sample_data.sql

-- ============================================================================
-- Verify Installation
-- ============================================================================

\echo ''
\echo '============================================================================'
\echo 'Database initialization complete!'
\echo '============================================================================'
\echo ''
\echo 'Extensions installed:'
SELECT extname, extversion FROM pg_extension WHERE extname IN ('postgis', 'pg_stat_statements', 'uuid-ossp');

\echo ''
\echo 'Tables created:'
SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename;

\echo ''
\echo 'Spatial indexes:'
SELECT
    schemaname,
    tablename,
    indexname
FROM pg_indexes
WHERE indexdef LIKE '%GIST%'
ORDER BY tablename;

\echo ''
\echo '============================================================================'
