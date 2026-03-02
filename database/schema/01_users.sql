-- ============================================================================
-- Table: users
-- Purpose: User accounts and authentication
-- ============================================================================

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,  -- bcrypt hash
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    organization VARCHAR(200),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',  -- USER, ADMIN

    -- Storage quota
    storage_quota_mb INTEGER NOT NULL DEFAULT 100,
    storage_used_mb NUMERIC(10,2) NOT NULL DEFAULT 0,

    -- Account status
    is_active BOOLEAN NOT NULL DEFAULT true,
    is_email_verified BOOLEAN NOT NULL DEFAULT false,
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

-- Indexes
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_active ON users(is_active);

-- Comments
COMMENT ON TABLE users IS 'User accounts with authentication and storage quotas';
COMMENT ON COLUMN users.password_hash IS 'Bcrypt hash with salt (never store plain text)';
COMMENT ON COLUMN users.storage_quota_mb IS 'Enforced at application layer and DB constraint';
COMMENT ON COLUMN users.failed_login_attempts IS 'Account lockout after 5 attempts';
COMMENT ON COLUMN users.locked_until IS 'Temporary lock expires after 30 minutes';
