# Database Schema & Migrations

Complete PostgreSQL + PostGIS database schema for WaterAccessOptimizer.

## Overview

This directory contains:
- **Schema files**: Individual table definitions
- **Migrations**: Version-controlled schema changes (ready for Flyway/Liquibase)
- **Triggers & Functions**: Utility functions and automatic timestamp updates
- **Seed data**: Development/staging/production seed data

## Quick Start

### 1. Initialize Database (Development)

```bash
# Create database
createdb water_optimizer

# Connect and run initialization
psql -d water_optimizer -f init.sql

# Load development seed data
psql -d water_optimizer -f seed/dev/default_users.sql
psql -d water_optimizer -f seed/dev/sample_data.sql
```

### 2. Verify Installation

```sql
-- Check extensions
SELECT extname, extversion FROM pg_extension
WHERE extname IN ('postgis', 'pg_stat_statements');

-- Check tables
\dt

-- Check spatial indexes
\di
```

## Directory Structure

```
database/
├── init.sql                      # Master initialization script
├── schema/                       # Individual table definitions
│   ├── 01_users.sql
│   ├── 02_uploads.sql
│   ├── 03_hydro_data.sql
│   ├── 04_community_data.sql
│   ├── 05_infrastructure_data.sql
│   ├── 06a_risk_assessments.sql
│   ├── 06b_assessment_datasets.sql
│   ├── 06c_risk_results.sql
│   ├── 07_projects.sql
│   ├── 12_audit_logs.sql
│   ├── 13_spatial_cache.sql
│   └── 14_data_validation_errors.sql
├── triggers/                     # Database triggers
│   └── update_timestamps.sql
├── functions/                    # Utility functions
│   └── normalize_capacity.sql
├── migrations/                   # Flyway/Liquibase migrations (future)
└── seed/                         # Seed data
    ├── dev/                      # Development seed data
    │   ├── default_users.sql
    │   └── sample_data.sql
    ├── staging/                  # Staging seed data (minimal)
    └── prod/                     # Production seed data (admin only)
```

## Database Schema

### MVP Tables

1. **users** - User accounts and authentication
2. **uploads** - File upload metadata and provenance
3. **hydro_data** - Hydrological measurements (water quality, levels)
4. **community_data** - Population and water access data
5. **infrastructure_data** - Water facilities and infrastructure
6. **risk_assessments** - Risk assessment metadata
7. **assessment_datasets** - Links assessments to source datasets
8. **risk_results** - Individual community risk scores
9. **projects** - User-created analysis projects
10. **audit_logs** - Security and compliance audit trail
11. **spatial_cache** - Precomputed spatial queries
12. **data_validation_errors** - Upload validation errors

### V1 Tables (Deferred)

- **llm_query_log** - LLM query tracking and cost monitoring
- **collaboration_sessions** - Real-time collaboration sessions
- **session_participants** - Session participation tracking
- **session_comments** - Comments and annotations
- **external_sync_log** - External API sync tracking

## PostGIS Spatial Data

### Coordinate System

All spatial data uses **SRID 4326** (WGS84 lat/lon):
- `coordinates GEOGRAPHY(POINT, 4326)` for points
- Distances returned in **meters** (not degrees)
- Accurate global distance calculations

### Spatial Indexes

All geography columns have **GIST indexes** for fast spatial queries:
```sql
CREATE INDEX idx_hydro_coordinates ON hydro_data USING GIST(coordinates);
```

### Common Spatial Queries

**Find points within radius:**
```sql
SELECT *
FROM hydro_data
WHERE ST_DWithin(
    coordinates,
    ST_SetSRID(ST_MakePoint(32.5825, 0.3476), 4326)::geography,
    10000  -- 10km in meters
);
```

**Find nearest neighbor:**
```sql
SELECT *
FROM infrastructure_data
WHERE operational_status = 'operational'
ORDER BY coordinates <-> ST_SetSRID(ST_MakePoint(32.5825, 0.3476), 4326)::geography
LIMIT 1;
```

See `docs/POSTGIS_SPATIAL_QUERIES.md` for comprehensive query patterns.

## Seed Data

### Development Environment

**Default users** (password is username + "123"):
- `admin@wateroptimizer.dev` / `admin123` (ADMIN role)
- `user@wateroptimizer.dev` / `password123` (USER role)
- `researcher@university.dev` / `researcher123` (USER role)

**Sample data**:
- 6 water quality measurements (Uganda scenario)
- 4 communities with varying water access levels
- 4 infrastructure facilities (2 operational, 1 broken, 1 under maintenance)

### Staging Environment

Minimal seed data - admin user only.

### Production Environment

No seed data - admin created via secure process.

## Database Sizing

| Scope | Data Size | With Indexes | % of 5GB Limit |
|-------|-----------|--------------|----------------|
| **MVP** | 118 MB | 240 MB | 5.8% |
| **V1** | 158 MB | 320 MB | 7.7% |

Growth projections:
- **Year 1**: ~500 MB (10% utilization)
- **Year 2**: ~1.5 GB (30% utilization)
- **Year 3**: ~3.5 GB (70% utilization)

## Performance Targets

| Query Type | Target | Example |
|------------|--------|---------|
| Point lookup by ID | <5ms | `SELECT * FROM users WHERE id = ?` |
| List view (paginated) | <50ms | `SELECT * FROM uploads LIMIT 20` |
| Spatial query (10km radius) | <100ms | Find all facilities within 10km |
| Risk assessment computation | <5s | Analyze 100 communities |

Monitor with `pg_stat_statements` extension.

## Backup & Recovery

**Backup Schedule**:
- Full backup: Daily at 2 AM
- Incremental backup: Every 6 hours
- Retention: 30 days

**Backup Commands**:
```bash
# Full backup
pg_dump -h localhost -U postgres -F c -b -v \
  -f backup_$(date +%Y%m%d).dump water_optimizer

# Restore
pg_restore -h localhost -U postgres -d water_optimizer \
  -v backup_20240120.dump
```

## Migrations (Future)

For production deployments, use **Flyway** or **Liquibase** for version-controlled migrations.

Example Flyway migration:
```sql
-- V1__initial_schema.sql
\i schema/01_users.sql
\i schema/02_uploads.sql
-- ...
```

See Agent 03 (DOMAIN_MODEL_DB_SCHEMA.md) for detailed migration strategy.

## Security

**Connection Security**:
- SSL/TLS required for all connections
- Separate users for each service with minimal privileges
- Rotate database passwords every 90 days

**User Privileges**:
```sql
-- Application user (read/write)
CREATE USER app_user WITH PASSWORD 'secure-password';
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO app_user;

-- Read-only user (analytics)
CREATE USER readonly_user WITH PASSWORD 'secure-password';
GRANT SELECT ON ALL TABLES IN SCHEMA public TO readonly_user;
```

## Monitoring

**Database size:**
```sql
SELECT pg_size_pretty(pg_database_size('water_optimizer'));
```

**Table sizes:**
```sql
SELECT
    tablename,
    pg_size_pretty(pg_total_relation_size(schemaname||'.'||tablename)) AS size
FROM pg_tables
WHERE schemaname = 'public'
ORDER BY pg_total_relation_size(schemaname||'.'||tablename) DESC;
```

**Slow queries:**
```sql
SELECT query, calls, total_time, mean_time
FROM pg_stat_statements
ORDER BY mean_time DESC
LIMIT 10;
```

## Troubleshooting

**PostGIS not found:**
```sql
CREATE EXTENSION IF NOT EXISTS postgis;
```

**Spatial index not being used:**
```sql
ANALYZE hydro_data;  -- Update table statistics
```

**Query too slow:**
```sql
EXPLAIN ANALYZE SELECT ...;  -- Check query plan
```

## References

- **Agent 03**: DOMAIN_MODEL_DB_SCHEMA.md (complete schema design)
- **PostGIS Queries**: docs/POSTGIS_SPATIAL_QUERIES.md
- **API Contracts**: docs/API_CONTRACTS_MVP.md
- **TypeScript Types**: docs/TYPESCRIPT_TYPES.ts
