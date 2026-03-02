-- Seed data for development and testing
-- This migration creates test users and sample data
-- Skip in production by setting FLYWAY_PLACEHOLDERS_ENV=prod

-- Create test user (password: Test123!)
-- Password hash is bcrypt encoded
INSERT INTO users (id, email, password_hash, full_name, organization, role, storage_quota_mb, storage_used_mb, is_active, email_verified, created_at)
VALUES
    ('550e8400-e29b-41d4-a716-446655440000',
     'test@wateroptimizer.org',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Test User',
     'Test Organization',
     'USER',
     500.00,
     0.00,
     true,
     true,
     NOW()),
    ('660e8400-e29b-41d4-a716-446655440001',
     'admin@wateroptimizer.org',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy',
     'Admin User',
     'WaterAccessOptimizer',
     'ADMIN',
     1000.00,
     0.00,
     true,
     true,
     NOW());

-- Create sample upload record
INSERT INTO uploads (id, user_id, data_type, filename, file_size_bytes, file_checksum, row_count, validation_status, error_count, warning_count, uploaded_at)
VALUES
    ('770e8400-e29b-41d4-a716-446655440002',
     '550e8400-e29b-41d4-a716-446655440000',
     'HYDRO',
     'sample_water_quality.csv',
     15360,
     'abc123def456',
     10,
     'PASSED',
     0,
     2,
     NOW());

-- Create sample hydro data (water quality measurements)
-- Using coordinates for East Africa region (Kenya, Tanzania, Uganda)
INSERT INTO hydro_data (upload_id, station_id, station_name, coordinates, measurement_date, ph, turbidity_ntu, tds_mg_l, arsenic_mg_l, fluoride_mg_l, nitrate_mg_l, ecoli_cfu_100ml)
VALUES
    ('770e8400-e29b-41d4-a716-446655440002', 'HYD001', 'Nairobi Station 1', ST_GeogFromText('POINT(36.8219 -1.2921)'), '2024-01-15', 7.2, 5.3, 320.0, 0.005, 0.8, 15.0, 0),
    ('770e8400-e29b-41d4-a716-446655440002', 'HYD002', 'Nairobi Station 2', ST_GeogFromText('POINT(36.8350 -1.3000)'), '2024-01-16', 6.8, 8.2, 450.0, 0.012, 1.2, 25.0, 5),
    ('770e8400-e29b-41d4-a716-446655440002', 'HYD003', 'Mombasa Station 1', ST_GeogFromText('POINT(39.6682 -4.0435)'), '2024-01-17', 7.5, 3.1, 280.0, 0.003, 0.5, 10.0, 0),
    ('770e8400-e29b-41d4-a716-446655440002', 'HYD004', 'Kisumu Station 1', ST_GeogFromText('POINT(34.7617 -0.0917)'), '2024-01-18', 6.9, 12.5, 520.0, 0.008, 1.8, 35.0, 15),
    ('770e8400-e29b-41d4-a716-446655440002', 'HYD005', 'Kampala Station 1', ST_GeogFromText('POINT(32.5825 0.3476)'), '2024-01-19', 7.1, 6.8, 380.0, 0.006, 0.9, 20.0, 3);

-- Create sample community data
INSERT INTO community_data (upload_id, community_name, coordinates, population, households, water_access_level, primary_water_source, distance_to_water_m, service_level)
VALUES
    ('770e8400-e29b-41d4-a716-446655440002', 'Kibera', ST_GeogFromText('POINT(36.7869 -1.3133)'), 250000, 50000, 'LIMITED', 'BOREHOLE', 450.0, 'LIMITED'),
    ('770e8400-e29b-41d4-a716-446655440002', 'Mathare', ST_GeogFromText('POINT(36.8587 -1.2621)'), 180000, 36000, 'UNIMPROVED', 'SURFACE_WATER', 800.0, 'UNIMPROVED'),
    ('770e8400-e29b-41d4-a716-446655440002', 'Mukuru', ST_GeogFromText('POINT(36.8842 -1.3067)'), 120000, 24000, 'LIMITED', 'PIPED_WATER', 200.0, 'BASIC'),
    ('770e8400-e29b-41d4-a716-446655440002', 'Korogocho', ST_GeogFromText('POINT(36.8827 -1.2548)'), 60000, 12000, 'UNIMPROVED', 'BOREHOLE', 1200.0, 'LIMITED');

-- Create sample infrastructure data
INSERT INTO infrastructure_data (upload_id, facility_id, facility_name, facility_type, coordinates, operational_status, capacity_liters_per_day, installation_year, last_maintenance_date)
VALUES
    ('770e8400-e29b-41d4-a716-446655440002', 'INF001', 'Nairobi Central Borehole', 'BOREHOLE', ST_GeogFromText('POINT(36.8219 -1.2840)'), 'OPERATIONAL', 50000.0, 2015, '2024-01-10'),
    ('770e8400-e29b-41d4-a716-446655440002', 'INF002', 'Eastlands Water Kiosk', 'WATER_KIOSK', ST_GeogFromText('POINT(36.8900 -1.2700)'), 'OPERATIONAL', 10000.0, 2018, '2023-12-15'),
    ('770e8400-e29b-41d4-a716-446655440002', 'INF003', 'Mathare Handpump', 'HANDPUMP', ST_GeogFromText('POINT(36.8600 -1.2600)'), 'PARTIALLY_OPERATIONAL', 5000.0, 2012, '2023-06-20'),
    ('770e8400-e29b-41d4-a716-446655440002', 'INF004', 'Kibera Treatment Plant', 'TREATMENT_PLANT', ST_GeogFromText('POINT(36.7800 -1.3100)'), 'OPERATIONAL', 100000.0, 2019, '2024-01-05');

-- Comments
COMMENT ON TABLE users IS 'Note: Test user password is "Test123!" - change in production!';
