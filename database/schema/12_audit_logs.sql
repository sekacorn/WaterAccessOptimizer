-- ============================================================================
-- Table: audit_logs
-- Purpose: Security and compliance audit trail
-- ============================================================================

CREATE TABLE audit_logs (
    id BIGSERIAL PRIMARY KEY,
    user_id UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Event details
    event_type VARCHAR(100) NOT NULL,  -- login, logout, upload, delete, role_change, etc.
    event_category VARCHAR(50) NOT NULL,  -- auth, data, admin, api
    resource_type VARCHAR(50),  -- user, upload, assessment
    resource_id VARCHAR(255),

    -- Request context
    ip_address INET,
    user_agent TEXT,
    http_method VARCHAR(10),
    endpoint VARCHAR(255),
    status_code INTEGER,

    -- Additional details
    details JSONB,

    -- Timestamp
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),

    -- Constraints
    CONSTRAINT check_event_category CHECK (
        event_category IN ('auth', 'data', 'admin', 'api', 'security')
    )
);

-- Indexes
CREATE INDEX idx_audit_user_id ON audit_logs(user_id);
CREATE INDEX idx_audit_event_type ON audit_logs(event_type);
CREATE INDEX idx_audit_category ON audit_logs(event_category);
CREATE INDEX idx_audit_created_at ON audit_logs(created_at DESC);
CREATE INDEX idx_audit_resource ON audit_logs(resource_type, resource_id);

-- Comments
COMMENT ON TABLE audit_logs IS 'Security and compliance audit trail for all user actions';
COMMENT ON COLUMN audit_logs.event_category IS 'Event category: auth, data, admin, api, security';
COMMENT ON COLUMN audit_logs.details IS 'JSONB for flexible event-specific details';
