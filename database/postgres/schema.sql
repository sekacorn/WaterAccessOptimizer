-- WaterAccessOptimizer PostgreSQL Schema
-- Creates tables for hydrological data, community data, infrastructure data, user sessions, and collaboration

-- Enable extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "postgis";

-- Enterprises table for organization accounts
CREATE TABLE IF NOT EXISTS enterprises (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(255) NOT NULL,
    organization_type VARCHAR(100), -- NGO, Government, Private, Research
    country VARCHAR(100),
    region VARCHAR(100),
    contact_email VARCHAR(255) NOT NULL,
    contact_phone VARCHAR(50),
    subscription_tier VARCHAR(50) DEFAULT 'FREE', -- FREE, BASIC, PROFESSIONAL, ENTERPRISE
    max_users INTEGER DEFAULT 10,
    max_data_storage_gb INTEGER DEFAULT 5,
    features JSONB, -- JSON object with enabled features
    billing_info JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_enterprise_active ON enterprises(is_active);
CREATE INDEX idx_enterprise_tier ON enterprises(subscription_tier);

-- Users table with enhanced role management
CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enterprise_id UUID REFERENCES enterprises(id) ON DELETE SET NULL,
    username VARCHAR(100) UNIQUE NOT NULL,
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    phone VARCHAR(50),
    mbti_type VARCHAR(4) CHECK (mbti_type IN ('ENTJ', 'INFP', 'INFJ', 'ESTP', 'INTJ', 'INTP', 'ISTJ', 'ESFJ', 'ISFP', 'ENTP', 'ISFJ', 'ESFP', 'ENFJ', 'ESTJ', 'ISTP')),
    role VARCHAR(50) DEFAULT 'USER' CHECK (role IN ('USER', 'MODERATOR', 'ADMIN', 'SUPER_ADMIN', 'ENTERPRISE_ADMIN')),
    permissions JSONB, -- Custom permissions array
    profile_picture_url VARCHAR(500),
    bio TEXT,
    organization VARCHAR(255),
    country VARCHAR(100),
    is_active BOOLEAN DEFAULT TRUE,
    is_verified BOOLEAN DEFAULT FALSE,
    email_verified BOOLEAN DEFAULT FALSE,
    last_login TIMESTAMP,
    login_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_user_enterprise ON users(enterprise_id);
CREATE INDEX idx_user_role ON users(role);
CREATE INDEX idx_user_active ON users(is_active);

-- Hydrological data table
CREATE TABLE IF NOT EXISTS hydro_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    source VARCHAR(100), -- USGS, WHO, etc.
    data_type VARCHAR(50), -- water_quality, aquifer_level, rainfall, etc.
    location GEOGRAPHY(POINT, 4326),
    location_name VARCHAR(255),
    measurement_value DECIMAL(10, 4),
    measurement_unit VARCHAR(50),
    measurement_date TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_hydro_location ON hydro_data USING GIST(location);
CREATE INDEX idx_hydro_type ON hydro_data(data_type);
CREATE INDEX idx_hydro_date ON hydro_data(measurement_date);

-- Community data table
CREATE TABLE IF NOT EXISTS community_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    community_name VARCHAR(255),
    location GEOGRAPHY(POINT, 4326),
    population INTEGER,
    water_access_level VARCHAR(50), -- no_access, basic, limited, safely_managed
    access_points JSONB, -- Array of water access point locations
    needs_assessment JSONB,
    source VARCHAR(100), -- OpenStreetMap, local survey, etc.
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_community_location ON community_data USING GIST(location);
CREATE INDEX idx_community_access ON community_data(water_access_level);

-- Infrastructure data table
CREATE TABLE IF NOT EXISTS infrastructure_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    facility_type VARCHAR(100), -- treatment_plant, pipeline, pump_station, reservoir
    facility_name VARCHAR(255),
    location GEOGRAPHY(POINT, 4326),
    capacity DECIMAL(12, 2),
    capacity_unit VARCHAR(50),
    operational_status VARCHAR(50), -- operational, maintenance, non_operational
    service_area GEOGRAPHY(POLYGON, 4326),
    construction_date DATE,
    last_maintenance TIMESTAMP,
    metadata JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_infra_location ON infrastructure_data USING GIST(location);
CREATE INDEX idx_infra_type ON infrastructure_data(facility_type);
CREATE INDEX idx_infra_status ON infrastructure_data(operational_status);

-- Water management predictions table
CREATE TABLE IF NOT EXISTS water_predictions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    prediction_type VARCHAR(50), -- availability, quality, management_strategy
    location GEOGRAPHY(POINT, 4326),
    prediction_result JSONB,
    confidence_score DECIMAL(5, 4),
    input_data_ids JSONB, -- References to hydro, community, infra data
    model_version VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_prediction_type ON water_predictions(prediction_type);
CREATE INDEX idx_prediction_location ON water_predictions USING GIST(location);

-- User sessions table
CREATE TABLE IF NOT EXISTS user_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    session_name VARCHAR(255),
    session_data JSONB,
    annotations JSONB,
    bookmarks JSONB,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_session_user ON user_sessions(user_id);

-- Visualizations table
CREATE TABLE IF NOT EXISTS visualizations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    session_id UUID REFERENCES user_sessions(id) ON DELETE CASCADE,
    visualization_type VARCHAR(50), -- 3d_hydro_map, access_network, quality_zones
    title VARCHAR(255),
    description TEXT,
    config JSONB,
    data_snapshot JSONB,
    export_formats VARCHAR(50)[], -- png, svg, stl
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_viz_user ON visualizations(user_id);
CREATE INDEX idx_viz_session ON visualizations(session_id);

-- Collaboration sessions table
CREATE TABLE IF NOT EXISTS collaboration_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_name VARCHAR(255),
    creator_id UUID REFERENCES users(id) ON DELETE CASCADE,
    participants JSONB, -- Array of user IDs
    shared_data JSONB,
    chat_history JSONB,
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_collab_creator ON collaboration_sessions(creator_id);
CREATE INDEX idx_collab_active ON collaboration_sessions(active);

-- User actions log (for collaboration)
CREATE TABLE IF NOT EXISTS user_actions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    session_id UUID REFERENCES collaboration_sessions(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    action_type VARCHAR(50), -- annotation, data_update, visualization_change
    action_data JSONB,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_action_session ON user_actions(session_id);
CREATE INDEX idx_action_timestamp ON user_actions(timestamp);

-- LLM query history
CREATE TABLE IF NOT EXISTS llm_queries (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    query_text TEXT,
    query_context JSONB,
    response_text TEXT,
    response_metadata JSONB,
    mbti_tailored BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_llm_user ON llm_queries(user_id);
CREATE INDEX idx_llm_created ON llm_queries(created_at);

-- Error logs for troubleshooting
CREATE TABLE IF NOT EXISTS error_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    error_type VARCHAR(100),
    error_message TEXT,
    stack_trace TEXT,
    context JSONB,
    resolved BOOLEAN DEFAULT FALSE,
    resolution_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    resolved_at TIMESTAMP
);

CREATE INDEX idx_error_user ON error_logs(user_id);
CREATE INDEX idx_error_type ON error_logs(error_type);
CREATE INDEX idx_error_resolved ON error_logs(resolved);

-- Resource monitoring logs
CREATE TABLE IF NOT EXISTS resource_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    service_name VARCHAR(100),
    cpu_usage DECIMAL(5, 2),
    memory_usage DECIMAL(5, 2),
    gpu_usage DECIMAL(5, 2),
    active_threads INTEGER,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_resource_service ON resource_logs(service_name);
CREATE INDEX idx_resource_timestamp ON resource_logs(timestamp);

-- Create trigger function for updated_at
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

-- Apply updated_at triggers
CREATE TRIGGER update_users_updated_at BEFORE UPDATE ON users
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_community_updated_at BEFORE UPDATE ON community_data
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_infrastructure_updated_at BEFORE UPDATE ON infrastructure_data
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_sessions_updated_at BEFORE UPDATE ON user_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_collab_updated_at BEFORE UPDATE ON collaboration_sessions
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Permissions table for fine-grained access control
CREATE TABLE IF NOT EXISTS permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL,
    resource VARCHAR(100) NOT NULL, -- data, visualization, collaboration, users, enterprises
    action VARCHAR(50) NOT NULL, -- create, read, update, delete, export, moderate
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_permission_resource ON permissions(resource);

-- Role permissions mapping
CREATE TABLE IF NOT EXISTS role_permissions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    role VARCHAR(50) NOT NULL,
    permission_id UUID REFERENCES permissions(id) ON DELETE CASCADE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(role, permission_id)
);

CREATE INDEX idx_role_permission ON role_permissions(role);

-- User invitations for enterprise/moderator assignments
CREATE TABLE IF NOT EXISTS user_invitations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) NOT NULL,
    invited_by UUID REFERENCES users(id) ON DELETE CASCADE,
    enterprise_id UUID REFERENCES enterprises(id) ON DELETE CASCADE,
    role VARCHAR(50) DEFAULT 'USER',
    invitation_token VARCHAR(255) UNIQUE NOT NULL,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, ACCEPTED, EXPIRED, CANCELLED
    expires_at TIMESTAMP NOT NULL,
    accepted_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_invitation_email ON user_invitations(email);
CREATE INDEX idx_invitation_token ON user_invitations(invitation_token);
CREATE INDEX idx_invitation_status ON user_invitations(status);

-- Audit logs for tracking user actions (compliance and security)
CREATE TABLE IF NOT EXISTS audit_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    enterprise_id UUID REFERENCES enterprises(id) ON DELETE SET NULL,
    action VARCHAR(100) NOT NULL, -- login, logout, data_upload, user_create, etc.
    resource_type VARCHAR(100), -- user, data, visualization, enterprise
    resource_id UUID,
    ip_address VARCHAR(45),
    user_agent TEXT,
    details JSONB,
    severity VARCHAR(20) DEFAULT 'INFO', -- INFO, WARNING, ERROR, CRITICAL
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_user ON audit_logs(user_id);
CREATE INDEX idx_audit_enterprise ON audit_logs(enterprise_id);
CREATE INDEX idx_audit_action ON audit_logs(action);
CREATE INDEX idx_audit_created ON audit_logs(created_at);
CREATE INDEX idx_audit_severity ON audit_logs(severity);

-- Moderation actions table
CREATE TABLE IF NOT EXISTS moderation_actions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    moderator_id UUID REFERENCES users(id) ON DELETE SET NULL,
    target_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    action_type VARCHAR(50) NOT NULL, -- WARN, SUSPEND, BAN, RESTORE, DELETE_CONTENT
    reason TEXT,
    details JSONB,
    duration_hours INTEGER, -- For temporary suspensions
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP
);

CREATE INDEX idx_moderation_moderator ON moderation_actions(moderator_id);
CREATE INDEX idx_moderation_target ON moderation_actions(target_user_id);
CREATE INDEX idx_moderation_active ON moderation_actions(is_active);

-- Reports table for user-reported issues
CREATE TABLE IF NOT EXISTS reports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    reporter_id UUID REFERENCES users(id) ON DELETE SET NULL,
    reported_user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    reported_resource_type VARCHAR(50), -- user, data, visualization, comment
    reported_resource_id UUID,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING', -- PENDING, UNDER_REVIEW, RESOLVED, DISMISSED
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    resolution_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_report_reporter ON reports(reporter_id);
CREATE INDEX idx_report_reported_user ON reports(reported_user_id);
CREATE INDEX idx_report_status ON reports(status);

-- Enterprise usage statistics
CREATE TABLE IF NOT EXISTS enterprise_usage (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enterprise_id UUID REFERENCES enterprises(id) ON DELETE CASCADE,
    date DATE NOT NULL,
    active_users INTEGER DEFAULT 0,
    data_uploads INTEGER DEFAULT 0,
    data_storage_gb DECIMAL(10, 2) DEFAULT 0,
    api_calls INTEGER DEFAULT 0,
    visualizations_created INTEGER DEFAULT 0,
    predictions_made INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(enterprise_id, date)
);

CREATE INDEX idx_usage_enterprise ON enterprise_usage(enterprise_id);
CREATE INDEX idx_usage_date ON enterprise_usage(date);

-- Apply updated_at trigger to enterprises
CREATE TRIGGER update_enterprises_updated_at BEFORE UPDATE ON enterprises
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default permissions
INSERT INTO permissions (name, resource, action, description) VALUES
    ('data.create', 'data', 'create', 'Upload and create water data'),
    ('data.read', 'data', 'read', 'View water data'),
    ('data.update', 'data', 'update', 'Edit water data'),
    ('data.delete', 'data', 'delete', 'Delete water data'),
    ('data.export', 'data', 'export', 'Export water data'),
    ('visualization.create', 'visualization', 'create', 'Create visualizations'),
    ('visualization.read', 'visualization', 'read', 'View visualizations'),
    ('visualization.export', 'visualization', 'export', 'Export visualizations'),
    ('collaboration.create', 'collaboration', 'create', 'Create collaboration sessions'),
    ('collaboration.join', 'collaboration', 'join', 'Join collaboration sessions'),
    ('users.read', 'users', 'read', 'View user profiles'),
    ('users.create', 'users', 'create', 'Create new users'),
    ('users.update', 'users', 'update', 'Update user information'),
    ('users.delete', 'users', 'delete', 'Delete users'),
    ('users.moderate', 'users', 'moderate', 'Moderate user content and behavior'),
    ('enterprise.read', 'enterprise', 'read', 'View enterprise information'),
    ('enterprise.update', 'enterprise', 'update', 'Update enterprise settings'),
    ('enterprise.manage_users', 'enterprise', 'manage_users', 'Manage enterprise users'),
    ('reports.read', 'reports', 'read', 'View reports'),
    ('reports.resolve', 'reports', 'resolve', 'Resolve reports'),
    ('system.admin', 'system', 'admin', 'Full system administration')
ON CONFLICT (name) DO NOTHING;

-- Assign permissions to roles
-- USER role - basic permissions
INSERT INTO role_permissions (role, permission_id)
SELECT 'USER', id FROM permissions WHERE name IN (
    'data.create', 'data.read', 'data.update', 'data.delete', 'data.export',
    'visualization.create', 'visualization.read', 'visualization.export',
    'collaboration.create', 'collaboration.join', 'users.read'
) ON CONFLICT DO NOTHING;

-- MODERATOR role - includes user permissions plus moderation
INSERT INTO role_permissions (role, permission_id)
SELECT 'MODERATOR', id FROM permissions WHERE name IN (
    'data.create', 'data.read', 'data.update', 'data.delete', 'data.export',
    'visualization.create', 'visualization.read', 'visualization.export',
    'collaboration.create', 'collaboration.join',
    'users.read', 'users.moderate',
    'reports.read', 'reports.resolve'
) ON CONFLICT DO NOTHING;

-- ADMIN role - includes moderator permissions plus user management
INSERT INTO role_permissions (role, permission_id)
SELECT 'ADMIN', id FROM permissions WHERE name IN (
    'data.create', 'data.read', 'data.update', 'data.delete', 'data.export',
    'visualization.create', 'visualization.read', 'visualization.export',
    'collaboration.create', 'collaboration.join',
    'users.read', 'users.create', 'users.update', 'users.delete', 'users.moderate',
    'enterprise.read', 'reports.read', 'reports.resolve'
) ON CONFLICT DO NOTHING;

-- ENTERPRISE_ADMIN role - manage enterprise and its users
INSERT INTO role_permissions (role, permission_id)
SELECT 'ENTERPRISE_ADMIN', id FROM permissions WHERE name IN (
    'data.create', 'data.read', 'data.update', 'data.delete', 'data.export',
    'visualization.create', 'visualization.read', 'visualization.export',
    'collaboration.create', 'collaboration.join',
    'users.read', 'users.create', 'users.update',
    'enterprise.read', 'enterprise.update', 'enterprise.manage_users'
) ON CONFLICT DO NOTHING;

-- SUPER_ADMIN role - all permissions
INSERT INTO role_permissions (role, permission_id)
SELECT 'SUPER_ADMIN', id FROM permissions ON CONFLICT DO NOTHING;

-- Insert default super admin user (password: admin123 - CHANGE IN PRODUCTION)
INSERT INTO users (username, email, password_hash, first_name, last_name, mbti_type, role, is_active, is_verified, email_verified)
VALUES ('admin', 'admin@wateraccess.org', '$2a$10$rXKb7K8zPqE5qZ9yW0l5YO5xF8zH8vQ8vQ8vQ8vQ8vQ8vQ8vQ8vQ8', 'System', 'Administrator', 'ENTJ', 'SUPER_ADMIN', true, true, true)
ON CONFLICT (username) DO NOTHING;

-- Insert sample moderator (password: moderator123 - CHANGE IN PRODUCTION)
INSERT INTO users (username, email, password_hash, first_name, last_name, mbti_type, role, is_active, is_verified, email_verified)
VALUES ('moderator', 'moderator@wateraccess.org', '$2a$10$rXKb7K8zPqE5qZ9yW0l5YO5xF8zH8vQ8vQ8vQ8vQ8vQ8vQ8vQ8vQ8', 'Content', 'Moderator', 'ISTJ', 'MODERATOR', true, true, true)
ON CONFLICT (username) DO NOTHING;

-- Multi-Factor Authentication (MFA) table
CREATE TABLE IF NOT EXISTS user_mfa (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE UNIQUE,
    mfa_enabled BOOLEAN DEFAULT FALSE,
    mfa_type VARCHAR(20) DEFAULT 'TOTP', -- TOTP (Google Authenticator), SMS, EMAIL
    secret_key VARCHAR(255), -- Encrypted TOTP secret
    backup_codes JSONB, -- Array of encrypted backup codes
    phone_number VARCHAR(50), -- For SMS MFA
    verified BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_used TIMESTAMP
);

CREATE INDEX idx_mfa_user ON user_mfa(user_id);
CREATE INDEX idx_mfa_enabled ON user_mfa(mfa_enabled);

-- MFA verification attempts (security tracking)
CREATE TABLE IF NOT EXISTS mfa_attempts (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    attempt_type VARCHAR(20), -- login, setup, disable
    success BOOLEAN,
    ip_address VARCHAR(45),
    user_agent TEXT,
    attempted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_mfa_attempts_user ON mfa_attempts(user_id);
CREATE INDEX idx_mfa_attempts_date ON mfa_attempts(attempted_at);

-- Trusted devices (remember this device feature)
CREATE TABLE IF NOT EXISTS trusted_devices (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    device_name VARCHAR(255),
    device_fingerprint VARCHAR(255) UNIQUE NOT NULL, -- Browser fingerprint
    ip_address VARCHAR(45),
    user_agent TEXT,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP, -- Devices expire after 30 days
    last_used TIMESTAMP
);

CREATE INDEX idx_trusted_device_user ON trusted_devices(user_id);
CREATE INDEX idx_trusted_device_fingerprint ON trusted_devices(device_fingerprint);
CREATE INDEX idx_trusted_device_active ON trusted_devices(is_active);

-- Apply updated_at trigger to user_mfa
CREATE TRIGGER update_user_mfa_updated_at BEFORE UPDATE ON user_mfa
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Single Sign-On (SSO) Configuration for Enterprises
CREATE TABLE IF NOT EXISTS sso_configurations (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enterprise_id UUID REFERENCES enterprises(id) ON DELETE CASCADE UNIQUE,
    sso_enabled BOOLEAN DEFAULT FALSE,
    sso_type VARCHAR(20) DEFAULT 'SAML', -- SAML, OIDC, OAuth2

    -- SAML Configuration
    saml_entity_id VARCHAR(500),
    saml_sso_url VARCHAR(500), -- Identity Provider SSO URL
    saml_slo_url VARCHAR(500), -- Single Logout URL
    saml_certificate TEXT, -- X.509 certificate from IdP
    saml_name_id_format VARCHAR(100), -- urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress

    -- OAuth/OIDC Configuration
    oauth_client_id VARCHAR(255),
    oauth_client_secret VARCHAR(500), -- Encrypted
    oauth_authorization_endpoint VARCHAR(500),
    oauth_token_endpoint VARCHAR(500),
    oauth_userinfo_endpoint VARCHAR(500),
    oauth_scope VARCHAR(255) DEFAULT 'openid email profile',

    -- Common Settings
    auto_provision_users BOOLEAN DEFAULT TRUE, -- Auto-create users on first SSO login
    require_sso BOOLEAN DEFAULT FALSE, -- Force SSO for all enterprise users
    allowed_domains JSONB, -- Array of allowed email domains
    attribute_mapping JSONB, -- Map IdP attributes to user fields

    -- Metadata
    metadata JSONB,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_sync TIMESTAMP
);

CREATE INDEX idx_sso_enterprise ON sso_configurations(enterprise_id);
CREATE INDEX idx_sso_active ON sso_configurations(is_active);

-- SSO Sessions (track active SSO sessions)
CREATE TABLE IF NOT EXISTS sso_sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    enterprise_id UUID REFERENCES enterprises(id) ON DELETE CASCADE,
    session_index VARCHAR(255), -- SAML SessionIndex
    name_id VARCHAR(500), -- SAML NameID or OAuth subject
    sso_provider VARCHAR(50), -- okta, azure_ad, google, etc.
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    last_activity TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sso_session_user ON sso_sessions(user_id);
CREATE INDEX idx_sso_session_enterprise ON sso_sessions(enterprise_id);
CREATE INDEX idx_sso_session_index ON sso_sessions(session_index);

-- SSO Authentication Logs
CREATE TABLE IF NOT EXISTS sso_auth_logs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    enterprise_id UUID REFERENCES enterprises(id) ON DELETE SET NULL,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,
    email VARCHAR(255),
    sso_provider VARCHAR(50),
    auth_type VARCHAR(20), -- login, logout, refresh
    success BOOLEAN,
    error_message TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sso_auth_enterprise ON sso_auth_logs(enterprise_id);
CREATE INDEX idx_sso_auth_user ON sso_auth_logs(user_id);
CREATE INDEX idx_sso_auth_created ON sso_auth_logs(created_at);

-- Supported SSO Providers (pre-configured templates)
CREATE TABLE IF NOT EXISTS sso_providers (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name VARCHAR(100) UNIQUE NOT NULL, -- okta, azure_ad, google_workspace, onelogin
    display_name VARCHAR(100),
    provider_type VARCHAR(20), -- SAML, OIDC, OAuth2
    icon_url VARCHAR(500),
    documentation_url VARCHAR(500),
    default_config JSONB, -- Default configuration template
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_sso_provider_type ON sso_providers(provider_type);

-- Apply updated_at trigger to SSO configurations
CREATE TRIGGER update_sso_config_updated_at BEFORE UPDATE ON sso_configurations
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Insert default SSO providers
INSERT INTO sso_providers (name, display_name, provider_type, icon_url, documentation_url, default_config) VALUES
    ('okta', 'Okta', 'SAML', '/icons/okta.svg', 'https://docs.wateraccess.org/sso/okta', '{"name_id_format": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"}'::jsonb),
    ('azure_ad', 'Azure Active Directory', 'SAML', '/icons/azure.svg', 'https://docs.wateraccess.org/sso/azure', '{"name_id_format": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"}'::jsonb),
    ('google_workspace', 'Google Workspace', 'SAML', '/icons/google.svg', 'https://docs.wateraccess.org/sso/google', '{"name_id_format": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"}'::jsonb),
    ('onelogin', 'OneLogin', 'SAML', '/icons/onelogin.svg', 'https://docs.wateraccess.org/sso/onelogin', '{"name_id_format": "urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress"}'::jsonb),
    ('auth0', 'Auth0', 'OIDC', '/icons/auth0.svg', 'https://docs.wateraccess.org/sso/auth0', '{"scope": "openid email profile"}'::jsonb),
    ('keycloak', 'Keycloak', 'OIDC', '/icons/keycloak.svg', 'https://docs.wateraccess.org/sso/keycloak', '{"scope": "openid email profile"}'::jsonb)
ON CONFLICT (name) DO NOTHING;
