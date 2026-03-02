-- ============================================================================
-- Seed Data: Sample Data (Development Environment)
-- ============================================================================
-- Purpose: Realistic sample data for testing all features
-- Based on: Uganda rural water access scenario
-- ============================================================================

-- Create sample upload record
INSERT INTO uploads (id, user_id, filename, file_checksum, file_size_bytes, file_type, data_type, status, records_imported, uploaded_at)
VALUES (
    '880e8400-e29b-41d4-a716-446655440003',
    '660e8400-e29b-41d4-a716-446655440001',
    'uganda_water_survey_jan2024.csv',
    'sha256:abc123def456789',
    1024000,
    'csv',
    'hydro',
    'completed',
    50,
    NOW() - INTERVAL '7 days'
)
ON CONFLICT (id) DO NOTHING;

-- Create sample hydro data (water quality measurements)
-- Location: Central Uganda (Kampala region)
INSERT INTO hydro_data (upload_id, user_id, source, location_name, coordinates, data_type, parameter_name, measurement_value, measurement_unit, measurement_date)
VALUES
    -- Arsenic contamination (exceeds WHO guideline of 10 µg/L)
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Field Survey', 'Well Site Alpha',
     ST_SetSRID(ST_MakePoint(32.5825, 0.3476), 4326)::geography,
     'water_quality', 'arsenic', 75.5, 'µg/L', NOW() - INTERVAL '10 days'),

    -- pH (acceptable range: 6.5-8.5)
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Field Survey', 'Well Site Alpha',
     ST_SetSRID(ST_MakePoint(32.5825, 0.3476), 4326)::geography,
     'water_quality', 'pH', 8.2, 'pH', NOW() - INTERVAL '10 days'),

    -- TDS (acceptable: <600 mg/L)
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Field Survey', 'Well Site Alpha',
     ST_SetSRID(ST_MakePoint(32.5825, 0.3476), 4326)::geography,
     'water_quality', 'TDS', 425, 'mg/L', NOW() - INTERVAL '10 days'),

    -- River site with high turbidity
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Field Survey', 'River Crossing Site',
     ST_SetSRID(ST_MakePoint(32.5950, 0.3550), 4326)::geography,
     'water_quality', 'turbidity', 15.5, 'NTU', NOW() - INTERVAL '9 days'),

    -- Borehole with good quality
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Field Survey', 'Borehole Beta',
     ST_SetSRID(ST_MakePoint(32.6100, 0.3400), 4326)::geography,
     'water_quality', 'arsenic', 5.2, 'µg/L', NOW() - INTERVAL '8 days'),

    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Field Survey', 'Borehole Beta',
     ST_SetSRID(ST_MakePoint(32.6100, 0.3400), 4326)::geography,
     'water_quality', 'fluoride', 0.8, 'mg/L', NOW() - INTERVAL '8 days');

-- Create sample community data
INSERT INTO community_data (upload_id, user_id, community_name, coordinates, population, household_count, water_access_level, primary_water_source, source)
VALUES
    -- Village with limited access (>30 min to water)
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Kasubi Village',
     ST_SetSRID(ST_MakePoint(32.5800, 0.3450), 4326)::geography,
     1200, 240, 'limited', 'well', 'Field Survey'),

    -- Village with no safe water (using surface water)
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Nakawa Settlement',
     ST_SetSRID(ST_MakePoint(32.5900, 0.3550), 4326)::geography,
     850, 170, 'none', 'surface_water', 'Field Survey'),

    -- Village with basic access (<30 min to water)
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Makerere Community',
     ST_SetSRID(ST_MakePoint(32.6000, 0.3350), 4326)::geography,
     2100, 420, 'basic', 'borehole', 'Field Survey'),

    -- Small village with safely managed water
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'Kololo Estate',
     ST_SetSRID(ST_MakePoint(32.6100, 0.3380), 4326)::geography,
     450, 90, 'safely_managed', 'piped_water', 'Field Survey');

-- Create sample infrastructure data
INSERT INTO infrastructure_data (upload_id, user_id, facility_type, facility_name, coordinates, operational_status, capacity, capacity_unit, population_served, installation_date, source)
VALUES
    -- Operational borehole
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'borehole', 'Borehole Alpha',
     ST_SetSRID(ST_MakePoint(32.5825, 0.3476), 4326)::geography,
     'operational', 3000, 'liters_per_day', 500, '2020-03-15', 'Field Survey'),

    -- Non-operational borehole (broken pump)
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'borehole', 'Borehole Gamma',
     ST_SetSRID(ST_MakePoint(32.5901, 0.3512), 4326)::geography,
     'non_operational', 2500, 'liters_per_day', 0, '2018-06-20', 'Field Survey'),

    -- Water treatment plant
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'treatment_plant', 'Central Treatment Facility',
     ST_SetSRID(ST_MakePoint(32.6100, 0.3400), 4326)::geography,
     'operational', 50000, 'liters_per_day', 2500, '2019-01-10', 'Field Survey'),

    -- Well under maintenance
    ('880e8400-e29b-41d4-a716-446655440003', '660e8400-e29b-41d4-a716-446655440001',
     'well', 'Community Well Delta',
     ST_SetSRID(ST_MakePoint(32.5950, 0.3550), 4326)::geography,
     'under_maintenance', 1000, 'liters_per_day', 200, '2017-09-05', 'Field Survey');

-- Log audit events for data uploads
INSERT INTO audit_logs (user_id, event_type, event_category, resource_type, resource_id, details)
VALUES (
    '660e8400-e29b-41d4-a716-446655440001',
    'data_uploaded',
    'data',
    'upload',
    '880e8400-e29b-41d4-a716-446655440003',
    '{"filename": "uganda_water_survey_jan2024.csv", "records": 50, "data_type": "hydro"}'
);
