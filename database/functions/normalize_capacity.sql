-- ============================================================================
-- Function: normalize_capacity
-- Purpose: Convert infrastructure capacity to standardized unit (liters_per_day)
-- ============================================================================

CREATE OR REPLACE FUNCTION normalize_capacity(
    cap NUMERIC,
    unit VARCHAR
)
RETURNS NUMERIC AS $$
BEGIN
    RETURN CASE unit
        WHEN 'liters_per_day' THEN cap
        WHEN 'liters_per_hour' THEN cap * 24
        WHEN 'cubic_meters' THEN cap * 1000  -- Assuming daily for storage
        WHEN 'liters' THEN cap  -- Storage capacity
        ELSE NULL
    END;
END;
$$ LANGUAGE plpgsql IMMUTABLE;

-- Comments
COMMENT ON FUNCTION normalize_capacity(NUMERIC, VARCHAR) IS 'Converts infrastructure capacity to liters_per_day for standardized comparisons';

-- Example usage:
-- SELECT
--     facility_name,
--     capacity,
--     capacity_unit,
--     normalize_capacity(capacity, capacity_unit) AS normalized_capacity_lpd
-- FROM infrastructure_data;
