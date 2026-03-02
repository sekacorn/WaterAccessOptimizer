-- Create users table
-- Stores user accounts with authentication and storage quota management

CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(255),
    organization VARCHAR(255),
    role VARCHAR(50) NOT NULL DEFAULT 'USER',
    storage_quota_mb NUMERIC(10, 2) NOT NULL DEFAULT 500.00,
    storage_used_mb NUMERIC(10, 2) NOT NULL DEFAULT 0.00,
    is_active BOOLEAN NOT NULL DEFAULT true,
    email_verified BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    last_login_at TIMESTAMP,

    CONSTRAINT chk_storage_used CHECK (storage_used_mb >= 0),
    CONSTRAINT chk_storage_quota CHECK (storage_quota_mb > 0),
    CONSTRAINT chk_role CHECK (role IN ('USER', 'ADMIN', 'ANALYST'))
);

-- Indexes for users table
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_created_at ON users(created_at DESC);

-- Comments for documentation
COMMENT ON TABLE users IS 'User accounts with authentication credentials and storage quotas';
COMMENT ON COLUMN users.storage_quota_mb IS 'Maximum storage allocation in megabytes';
COMMENT ON COLUMN users.storage_used_mb IS 'Current storage usage in megabytes';
COMMENT ON COLUMN users.role IS 'User role: USER, ADMIN, or ANALYST';
