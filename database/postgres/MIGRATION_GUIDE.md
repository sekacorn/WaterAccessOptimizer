# Database Migration Guide - Old Schema → MVP Schema

**From**: `schema.sql` (V2 features, MBTI, Enterprise, SSO, MFA)
**To**: `V1_SCHEMA_MVP.sql` (MVP only, Agent 02 + 03 compliant)
**Date**: 2026-02-02
**Iteration**: 4

---

## Overview

This guide explains how to migrate from the old `schema.sql` (which includes many V2 features) to the new `V1_SCHEMA_MVP.sql` (MVP-focused schema based on Agent 02 and Agent 03).

**Key Changes**:
- Removed MBTI personalization (users.mbti_type, llm_queries.mbti_tailored)
- Removed enterprise features (enterprises table, subscription tiers)
- Removed 3 roles (MODERATOR, SUPER_ADMIN, ENTERPRISE_ADMIN) - kept USER, ADMIN only
- Removed V2 features (MFA, SSO, moderation, collaboration V1)
- Added proper schemas (auth_schema, data_schema, analysis_schema)
- Added MVP tables (uploads, risk_assessments, assessment_datasets, risk_results)
- Added security tables (refresh_tokens, failed_login_attempts, locked_until)
- Renamed fields to match Agent 03 (location → coordinates, etc.)

---

## Migration Strategy

### Option 1: Fresh Database (Recommended for MVP)

**When to use**: Starting fresh, no production data to preserve

**Steps**:
```bash
# 1. Drop existing database (WARNING: Deletes all data)
psql -U postgres -c "DROP DATABASE IF EXISTS wateraccess;"

# 2. Create new database
psql -U postgres -c "CREATE DATABASE wateraccess;"

# 3. Apply MVP schema
psql -U postgres -d wateraccess -f database/postgres/V1_SCHEMA_MVP.sql

# 4. Verify schemas created
psql -U postgres -d wateraccess -c "SELECT schema_name FROM information_schema.schemata WHERE schema_name LIKE '%_schema' ORDER BY schema_name;"

# Expected output:
#  schema_name
# --------------
#  analysis_schema
#  auth_schema
#  data_schema
```

**Verification**:
```bash
# Check PostGIS extension
psql -U postgres -d wateraccess -c "SELECT PostGIS_Full_Version();"

# Check tables
psql -U postgres -d wateraccess -c "
SELECT
    schemaname,
    tablename
FROM pg_tables
WHERE schemaname LIKE '%_schema'
ORDER BY schemaname, tablename;
"

# Expected output:
#     schemaname    |         tablename
# ------------------+----------------------------
#  analysis_schema  | assessment_datasets
#  analysis_schema  | risk_assessments
#  analysis_schema  | risk_results
#  auth_schema      | audit_logs
#  auth_schema      | refresh_tokens
#  auth_schema      | users
#  data_schema      | community_data
#  data_schema      | data_validation_errors
#  data_schema      | hydro_data
#  data_schema      | infrastructure_data
#  data_schema      | uploads
```

---

### Option 2: Data Migration (If Preserving Existing Data)

**When to use**: Have production data in old schema, need to migrate to MVP schema

**WARNING**: This is complex and risky. Test thoroughly on a copy of production database first.

**Steps**:

#### 1. Backup Existing Database

```bash
# Create timestamped backup
pg_dump -U postgres wateraccess > wateraccess_backup_$(date +%Y%m%d_%H%M%S).sql

# Verify backup
ls -lh wateraccess_backup_*.sql
```

#### 2. Create Temporary Migration Database

```bash
# Create temporary database
psql -U postgres -c "CREATE DATABASE wateraccess_migration;"

# Restore backup to temp database
psql -U postgres -d wateraccess_migration < wateraccess_backup_20260202_100000.sql

# Apply MVP schema to new database (different name)
psql -U postgres -c "CREATE DATABASE wateraccess_mvp;"
psql -U postgres -d wateraccess_mvp -f database/postgres/V1_SCHEMA_MVP.sql
```

#### 3. Migrate Users Table

**Field Mapping**:
- Old `users.username` → Dropped (MVP uses email only)
- Old `users.mbti_type` → Dropped (MBTI removed)
- Old `users.role` → Filter to USER/ADMIN only
- Old `users.enterprise_id` → Dropped (no enterprises in MVP)
- Old `users.phone` → Dropped (not in MVP)
- Old `users.permissions` → Dropped (no custom permissions in MVP)
- Add `users.storage_quota_mb` → Default 100
- Add `users.storage_used_mb` → Default 0
- Add `users.failed_login_attempts` → Default 0
- Add `users.locked_until` → NULL

**Migration SQL**:
```sql
-- Migrate users (USER and ADMIN roles only)
INSERT INTO wateraccess_mvp.auth_schema.users (
    id,
    email,
    password_hash,
    first_name,
    last_name,
    organization,
    role,
    storage_quota_mb,
    storage_used_mb,
    is_active,
    is_email_verified,
    failed_login_attempts,
    locked_until,
    created_at,
    updated_at,
    last_login_at
)
SELECT
    id,
    email,
    password_hash,
    first_name,
    last_name,
    organization,
    CASE
        WHEN role IN ('USER', 'ADMIN') THEN role
        WHEN role IN ('MODERATOR', 'SUPER_ADMIN', 'ENTERPRISE_ADMIN') THEN 'ADMIN'  -- Convert V2 roles to ADMIN
        ELSE 'USER'
    END as role,
    100 as storage_quota_mb,  -- Default quota
    0 as storage_used_mb,     -- Recalculate after data migration
    is_active,
    email_verified as is_email_verified,
    0 as failed_login_attempts,
    NULL as locked_until,
    created_at,
    updated_at,
    last_login as last_login_at
FROM wateraccess_migration.users
WHERE role IN ('USER', 'ADMIN', 'MODERATOR', 'SUPER_ADMIN', 'ENTERPRISE_ADMIN');  -- Include all, convert later

-- Verify user count
SELECT 'Old users:', COUNT(*) FROM wateraccess_migration.users;
SELECT 'New users:', COUNT(*) FROM wateraccess_mvp.auth_schema.users;
```

#### 4. Migrate Hydrological Data

**Field Mapping**:
- Old `hydro_data.location` → New `hydro_data.coordinates` (same PostGIS type)
- Old `hydro_data.id` (UUID) → New `hydro_data.id` (BIGSERIAL, regenerated)
- Add `hydro_data.upload_id` → NULL (no uploads table in old schema)

**Migration SQL**:
```sql
-- Migrate hydro_data
INSERT INTO wateraccess_mvp.data_schema.hydro_data (
    upload_id,
    user_id,
    source,
    location_name,
    coordinates,
    data_type,
    parameter_name,
    measurement_value,
    measurement_unit,
    measurement_date,
    depth_meters,
    notes,
    metadata,
    external_source_id,
    data_version,
    uploaded_at,
    last_updated_at
)
SELECT
    NULL as upload_id,  -- No uploads in old schema
    user_id,
    source,
    location_name,
    location as coordinates,  -- PostGIS GEOGRAPHY type preserved
    data_type,
    NULL as parameter_name,  -- Not in old schema
    measurement_value,
    measurement_unit,
    measurement_date,
    NULL as depth_meters,  -- Not in old schema
    NULL as notes,
    metadata,
    NULL as external_source_id,
    NULL as data_version,
    created_at as uploaded_at,
    created_at as last_updated_at
FROM wateraccess_migration.hydro_data;

-- Verify count
SELECT 'Old hydro_data:', COUNT(*) FROM wateraccess_migration.hydro_data;
SELECT 'New hydro_data:', COUNT(*) FROM wateraccess_mvp.data_schema.hydro_data;
```

#### 5. Migrate Community Data

**Migration SQL**:
```sql
-- Migrate community_data
INSERT INTO wateraccess_mvp.data_schema.community_data (
    upload_id,
    user_id,
    community_name,
    coordinates,
    population,
    household_count,
    water_access_level,
    primary_water_source,
    collection_date,
    notes,
    metadata,
    external_source_id,
    source,
    uploaded_at,
    last_updated_at
)
SELECT
    NULL as upload_id,
    user_id,
    community_name,
    location as coordinates,
    population,
    NULL as household_count,  -- Not in old schema
    water_access_level,
    NULL as primary_water_source,  -- Not in old schema
    NULL as collection_date,  -- Not in old schema
    NULL as notes,
    metadata,
    NULL as external_source_id,
    source,
    created_at as uploaded_at,
    updated_at as last_updated_at
FROM wateraccess_migration.community_data;

-- Verify count
SELECT 'Old community_data:', COUNT(*) FROM wateraccess_migration.community_data;
SELECT 'New community_data:', COUNT(*) FROM wateraccess_mvp.data_schema.community_data;
```

#### 6. Migrate Infrastructure Data

**Migration SQL**:
```sql
-- Migrate infrastructure_data
INSERT INTO wateraccess_mvp.data_schema.infrastructure_data (
    upload_id,
    user_id,
    facility_type,
    facility_name,
    coordinates,
    operational_status,
    capacity,
    capacity_unit,
    population_served,
    installation_date,
    last_maintenance_date,
    notes,
    metadata,
    external_source_id,
    source,
    uploaded_at,
    last_updated_at
)
SELECT
    NULL as upload_id,
    user_id,
    facility_type,
    facility_name,
    location as coordinates,
    operational_status,
    capacity,
    capacity_unit,
    NULL as population_served,  -- Not in old schema
    construction_date as installation_date,
    last_maintenance as last_maintenance_date,
    NULL as notes,
    metadata,
    NULL as external_source_id,
    'Manual Upload' as source,  -- Not in old schema
    created_at as uploaded_at,
    updated_at as last_updated_at
FROM wateraccess_migration.infrastructure_data;

-- Verify count
SELECT 'Old infrastructure_data:', COUNT(*) FROM wateraccess_migration.infrastructure_data;
SELECT 'New infrastructure_data:', COUNT(*) FROM wateraccess_mvp.data_schema.infrastructure_data;
```

#### 7. Migrate Audit Logs

**Migration SQL**:
```sql
-- Migrate audit_logs
INSERT INTO wateraccess_mvp.auth_schema.audit_logs (
    user_id,
    event_type,
    event_category,
    resource_type,
    resource_id,
    ip_address,
    user_agent,
    http_method,
    endpoint,
    status_code,
    details,
    created_at
)
SELECT
    user_id,
    action as event_type,  -- Old: action, New: event_type
    CASE
        WHEN action LIKE '%login%' OR action LIKE '%logout%' THEN 'auth'
        WHEN action LIKE '%upload%' OR action LIKE '%data%' THEN 'data'
        WHEN action LIKE '%user%' OR action LIKE '%role%' THEN 'admin'
        ELSE 'api'
    END as event_category,
    resource_type,
    resource_id::VARCHAR,  -- Old: UUID, New: VARCHAR
    ip_address::INET,  -- Old: VARCHAR, New: INET
    user_agent,
    NULL as http_method,  -- Not in old schema
    NULL as endpoint,  -- Not in old schema
    NULL as status_code,  -- Not in old schema
    details,
    created_at
FROM wateraccess_migration.audit_logs;

-- Verify count
SELECT 'Old audit_logs:', COUNT(*) FROM wateraccess_migration.audit_logs;
SELECT 'New audit_logs:', COUNT(*) FROM wateraccess_mvp.auth_schema.audit_logs;
```

#### 8. Drop Old Tables Not in MVP

**Tables to NOT migrate** (V2 features):
- `enterprises` - V2 feature
- `user_mfa` - V2 feature
- `mfa_attempts` - V2 feature
- `trusted_devices` - V2 feature
- `sso_configurations` - V2 feature
- `sso_sessions` - V2 feature
- `sso_auth_logs` - V2 feature
- `sso_providers` - V2 feature
- `permissions` - V2 feature
- `role_permissions` - V2 feature
- `user_invitations` - V2 feature
- `moderation_actions` - V2 feature
- `reports` - V2 feature
- `enterprise_usage` - V2 feature
- `collaboration_sessions` - V1 feature
- `user_actions` - V1 feature
- `llm_queries` - V1 feature
- `water_predictions` - V1 feature (use risk_results instead)
- `user_sessions` - V1 feature
- `visualizations` - V1 feature
- `error_logs` - Use audit_logs instead
- `resource_logs` - Use Prometheus metrics instead

#### 9. Recalculate Storage Usage

**After migrating data, recalculate each user's storage**:
```sql
-- Recalculate storage usage for each user
WITH user_storage AS (
    SELECT
        user_id,
        SUM(
            CASE
                WHEN upload_id IS NULL THEN 0.5  -- Migrated data, estimate 0.5 MB per 1000 rows
                ELSE 0  -- Will be calculated from uploads table later
            END
        ) / 1000.0 as estimated_mb
    FROM (
        SELECT user_id, upload_id FROM wateraccess_mvp.data_schema.hydro_data
        UNION ALL
        SELECT user_id, upload_id FROM wateraccess_mvp.data_schema.community_data
        UNION ALL
        SELECT user_id, upload_id FROM wateraccess_mvp.data_schema.infrastructure_data
    ) all_data
    GROUP BY user_id
)
UPDATE wateraccess_mvp.auth_schema.users u
SET storage_used_mb = COALESCE(us.estimated_mb, 0)
FROM user_storage us
WHERE u.id = us.user_id;

-- Verify storage calculations
SELECT
    email,
    storage_used_mb,
    storage_quota_mb,
    ROUND((storage_used_mb / storage_quota_mb * 100)::NUMERIC, 2) as percent_used
FROM wateraccess_mvp.auth_schema.users
WHERE storage_used_mb > 0
ORDER BY storage_used_mb DESC
LIMIT 10;
```

#### 10. Swap Databases

**WARNING**: This causes downtime. Schedule during maintenance window.

```bash
# Stop all application services
docker-compose stop api-gateway auth-service data-service worker-service

# Drop old database
psql -U postgres -c "DROP DATABASE wateraccess;"

# Rename migration database to production name
psql -U postgres -c "ALTER DATABASE wateraccess_mvp RENAME TO wateraccess;"

# Verify
psql -U postgres -d wateraccess -c "SELECT COUNT(*) FROM auth_schema.users;"

# Start services
docker-compose start api-gateway auth-service data-service worker-service
```

---

## Rollback Plan

If migration fails, restore from backup:

```bash
# Stop services
docker-compose stop

# Drop failed database
psql -U postgres -c "DROP DATABASE wateraccess;"

# Restore from backup
psql -U postgres -c "CREATE DATABASE wateraccess;"
psql -U postgres -d wateraccess < wateraccess_backup_20260202_100000.sql

# Start services
docker-compose start
```

---

## Post-Migration Verification

### 1. Verify Schema Structure

```bash
# Check schemas exist
psql -U postgres -d wateraccess -c "\dn"

# Expected output:
#        List of schemas
#       Name        |  Owner
# ------------------+----------
#  analysis_schema  | postgres
#  auth_schema      | postgres
#  data_schema      | postgres
#  public           | postgres
```

### 2. Verify Table Counts

```sql
-- Count tables in each schema
SELECT
    schemaname,
    COUNT(*) as table_count
FROM pg_tables
WHERE schemaname LIKE '%_schema'
GROUP BY schemaname
ORDER BY schemaname;

-- Expected output:
#     schemaname    | table_count
# ------------------+-------------
#  analysis_schema  |           3
#  auth_schema      |           3
#  data_schema      |           5
```

### 3. Verify User Roles

```sql
-- Check role distribution
SELECT
    role,
    COUNT(*) as count,
    COUNT(*) FILTER (WHERE is_active = true) as active_count
FROM auth_schema.users
GROUP BY role;

-- Expected output: Only USER and ADMIN roles
#   role  | count | active_count
# --------+-------+--------------
#  USER   |    98 |           95
#  ADMIN  |     2 |            2
```

### 4. Verify Spatial Indexes

```sql
-- Check spatial indexes exist
SELECT
    schemaname,
    tablename,
    indexname
FROM pg_indexes
WHERE indexname LIKE '%coordinates%'
ORDER BY schemaname, tablename;

-- Expected output:
#     schemaname    |      tablename       |          indexname
# ------------------+----------------------+-----------------------------
#  data_schema      | community_data       | idx_community_coordinates
#  data_schema      | hydro_data           | idx_hydro_coordinates
#  data_schema      | infrastructure_data  | idx_infrastructure_coordinates
```

### 5. Test PostGIS Queries

```sql
-- Test spatial query
SELECT COUNT(*)
FROM data_schema.hydro_data
WHERE ST_DWithin(
    coordinates,
    ST_GeogFromText('POINT(-118.25 34.05)'),
    10000  -- 10km in meters
);

-- Should return count without errors
```

### 6. Test Admin Login

```bash
# Test admin user login via API
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@wateroptimizer.local",
    "password": "ChangeMe123!"
  }'

# Expected: JWT token returned
```

---

## Breaking Changes

**Applications using old schema must update**:

1. **User Queries**: Update schema prefix
   ```sql
   -- Old
   SELECT * FROM users WHERE email = ?;

   -- New
   SELECT * FROM auth_schema.users WHERE email = ?;
   ```

2. **Data Queries**: Update table/column names
   ```sql
   -- Old
   SELECT id, location FROM hydro_data;

   -- New
   SELECT id, coordinates FROM data_schema.hydro_data;
   ```

3. **Role Checks**: Update role enum
   ```java
   // Old
   if (user.getRole().equals("SUPER_ADMIN") || user.getRole().equals("ADMIN")) { ... }

   // New
   if (user.getRole().equals("ADMIN")) { ... }
   ```

4. **MBTI References**: Remove all MBTI logic
   ```java
   // Old
   String mbtiType = user.getMbtiType();
   String response = llmService.queryWithMBTI(query, mbtiType);

   // New
   String response = llmService.query(query);  // No MBTI
   ```

---

## Migration Checklist

Before migration:
- [ ] Backup production database
- [ ] Test migration on copy of production database
- [ ] Verify rollback procedure works
- [ ] Schedule maintenance window
- [ ] Notify users of downtime

During migration:
- [ ] Stop all application services
- [ ] Run migration scripts
- [ ] Verify table counts
- [ ] Verify user roles
- [ ] Test admin login
- [ ] Test spatial queries

After migration:
- [ ] Start application services
- [ ] Smoke test critical paths (login, upload, analysis)
- [ ] Monitor error logs
- [ ] Verify Prometheus metrics
- [ ] Check Grafana dashboards

If issues:
- [ ] Execute rollback plan
- [ ] Restore from backup
- [ ] Restart services
- [ ] Investigate failures

---

## Migration Timeline

**Estimated Duration**:
- Fresh database (Option 1): 5 minutes
- Data migration (Option 2): 30-60 minutes (depends on data size)

**Recommended Approach**:
- **Development**: Use Option 1 (fresh database)
- **Staging**: Use Option 2 (data migration) to test procedure
- **Production**: Use Option 2 (data migration) after thorough testing

---

## Support

If you encounter issues during migration:
1. Check PostgreSQL logs: `docker logs wateroptimizer_postgres`
2. Verify PostGIS extension: `SELECT PostGIS_Full_Version();`
3. Check schema permissions: `\dn+` in psql
4. Review this guide's troubleshooting section
5. Restore from backup if unrecoverable

---

**Document Status**: [X]Complete
**Last Updated**: 2026-02-02 (Iteration 4)
**Tested**: No (pending Sprint 1 implementation)
