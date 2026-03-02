-- ============================================================================
-- Table: spatial_cache
-- Purpose: Performance optimization for expensive spatial queries
-- ============================================================================

CREATE TABLE spatial_cache (
    id BIGSERIAL PRIMARY KEY,
    cache_key VARCHAR(255) UNIQUE NOT NULL,
    cache_type VARCHAR(50) NOT NULL,  -- nearest_facility, coverage_area, density_grid

    -- Query parameters (for cache invalidation)
    user_id UUID,
    dataset_ids UUID[],
    bounding_box BOX2D,

    -- Cached results
    result JSONB NOT NULL,

    -- Cache management
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    expires_at TIMESTAMP NOT NULL,
    hit_count INTEGER NOT NULL DEFAULT 0,
    last_accessed_at TIMESTAMP NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_spatial_cache_key ON spatial_cache(cache_key);
CREATE INDEX idx_spatial_cache_expires ON spatial_cache(expires_at);

-- Comments
COMMENT ON TABLE spatial_cache IS 'Precomputed spatial queries for performance (1 hour TTL)';
COMMENT ON COLUMN spatial_cache.cache_key IS 'Hash of query parameters for cache lookup';
COMMENT ON COLUMN spatial_cache.expires_at IS 'Cache expiration time (default 1 hour)';
COMMENT ON COLUMN spatial_cache.hit_count IS 'Track cache hits for performance monitoring';
