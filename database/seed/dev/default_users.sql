-- ============================================================================
-- Seed Data: Default Users (Development Environment)
-- ============================================================================
-- WARNING: DO NOT run in production environment
-- These are test users with well-known passwords for development only
-- ============================================================================

-- Create admin user (password: admin123)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active, is_email_verified, storage_quota_mb)
VALUES (
    '550e8400-e29b-41d4-a716-446655440000',
    'admin@wateroptimizer.dev',
    '$2a$10$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lW4k8WvDfXNO',
    'Admin',
    'User',
    'ADMIN',
    true,
    true,
    1000  -- 1GB for admin
)
ON CONFLICT (email) DO NOTHING;

-- Create regular test user (password: password123)
INSERT INTO users (id, email, password_hash, first_name, last_name, role, is_active, is_email_verified)
VALUES (
    '660e8400-e29b-41d4-a716-446655440001',
    'user@wateroptimizer.dev',
    '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi',
    'Test',
    'User',
    'USER',
    true,
    true
)
ON CONFLICT (email) DO NOTHING;

-- Create researcher user (password: researcher123)
INSERT INTO users (id, email, password_hash, first_name, last_name, organization, role, is_active, is_email_verified, storage_quota_mb)
VALUES (
    '770e8400-e29b-41d4-a716-446655440002',
    'researcher@university.dev',
    '$2a$10$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewY5lW4k8WvDfXNO',
    'Dr. Jane',
    'Researcher',
    'University Research Lab',
    'USER',
    true,
    true,
    500
)
ON CONFLICT (email) DO NOTHING;

-- Log audit events for user creation
INSERT INTO audit_logs (user_id, event_type, event_category, details)
VALUES
    ('550e8400-e29b-41d4-a716-446655440000', 'user_created', 'admin', '{"role": "ADMIN", "environment": "dev"}'),
    ('660e8400-e29b-41d4-a716-446655440001', 'user_created', 'admin', '{"role": "USER", "environment": "dev"}'),
    ('770e8400-e29b-41d4-a716-446655440002', 'user_created', 'admin', '{"role": "USER", "environment": "dev"}');
