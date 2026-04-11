# PostGIS Spatial Queries Reference
> Status: database reference. Confirm schema compatibility before using these patterns in the currently packaged runtime.

**Version:** 1.0.0
**Last Updated:** 2026-02-03
**Status:** MVP (Iteration 7)
**Reference:** Agent 05 (GIS_3D_VISUALIZATION.md), Database V1_SCHEMA_MVP.sql

---

## Overview

This document provides PostGIS spatial query patterns for the WaterAccessOptimizer MVP. All spatial operations use the GEOGRAPHY type with SRID 4326 (WGS84).

**Key Concepts:**
- **GEOGRAPHY**: Spherical earth model, distances in meters
- **GEOMETRY**: Planar model, faster for visualization
- **SRID 4326**: WGS84 coordinate system (latitude/longitude)
- **SRID 3857**: Web Mercator (for distance calculations in meters)

---

## Database Schema Spatial Columns

From `database/postgres/V1_SCHEMA_MVP.sql`:

```sql
-- Community data with spatial column
CREATE TABLE data_schema.community_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    community_name VARCHAR(255) NOT NULL,
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(11, 7) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    population INTEGER,
    ...
);

-- Infrastructure data with spatial column
CREATE TABLE data_schema.infrastructure_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    facility_name VARCHAR(255),
    facility_type VARCHAR(50),
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(11, 7) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    functionality VARCHAR(50),
    ...
);

-- Water quality data with spatial column
CREATE TABLE data_schema.hydro_data (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    location_name VARCHAR(255),
    latitude DECIMAL(10, 7) NOT NULL,
    longitude DECIMAL(11, 7) NOT NULL,
    coordinates GEOGRAPHY(POINT, 4326) NOT NULL,
    parameter_name VARCHAR(50),
    measurement_value DECIMAL(15, 6),
    ...
);

-- Spatial indexes for performance
CREATE INDEX idx_community_coordinates ON data_schema.community_data USING GIST (coordinates);
CREATE INDEX idx_infrastructure_coordinates ON data_schema.infrastructure_data USING GIST (coordinates);
CREATE INDEX idx_hydro_coordinates ON data_schema.hydro_data USING GIST (coordinates);
```

---

## Common Spatial Query Patterns

### 1. Find Nearby Communities

**Use Case:** Find all communities within X kilometers of a point

```sql
-- Find communities within 10km of a specific location
SELECT
    id,
    community_name,
    population,
    ST_X(coordinates::geometry) AS longitude,
    ST_Y(coordinates::geometry) AS latitude,
    ST_Distance(
        coordinates,
        ST_SetSRID(ST_MakePoint(18.5053, -12.5047), 4326)::geography
    ) / 1000 AS distance_km
FROM data_schema.community_data
WHERE ST_DWithin(
    coordinates,
    ST_SetSRID(ST_MakePoint(18.5053, -12.5047), 4326)::geography,
    10000  -- 10km in meters
)
ORDER BY distance_km;
```

**Performance Notes:**
- `ST_DWithin` uses spatial index (fast)
- GEOGRAPHY type automatically handles spherical earth
- Distance calculations in meters

---

### 2. Find Nearest Infrastructure to Each Community

**Use Case:** Risk scoring - calculate access_distance component

```sql
-- Find nearest infrastructure for each community
WITH nearest AS (
    SELECT
        c.id AS community_id,
        c.community_name,
        c.coordinates AS community_coords,
        (
            SELECT i.id
            FROM data_schema.infrastructure_data i
            WHERE i.functionality = 'functional'
            ORDER BY i.coordinates <-> c.coordinates
            LIMIT 1
        ) AS nearest_infrastructure_id,
        (
            SELECT ST_Distance(i.coordinates, c.coordinates) / 1000
            FROM data_schema.infrastructure_data i
            WHERE i.functionality = 'functional'
            ORDER BY i.coordinates <-> c.coordinates
            LIMIT 1
        ) AS distance_km
    FROM data_schema.community_data c
)
SELECT * FROM nearest;
```

**Performance Notes:**
- `<->` operator uses KNN-GIST index (very fast)
- Returns distance to single nearest point
- Filter by `functionality = 'functional'` for operational facilities only

---

### 3. Count Functional Infrastructure Within Region

**Use Case:** Infrastructure risk component - count facilities serving an area

```sql
-- Count functional facilities within 5km of each community
SELECT
    c.id,
    c.community_name,
    COUNT(i.id) AS functional_facilities_nearby
FROM data_schema.community_data c
LEFT JOIN data_schema.infrastructure_data i ON
    ST_DWithin(c.coordinates, i.coordinates, 5000)  -- 5km
    AND i.functionality = 'functional'
GROUP BY c.id, c.community_name;
```

---

### 4. Bounding Box Query (for Map Viewport)

**Use Case:** Fetch data visible in current map view

```sql
-- Get all communities within map bounds
-- Bounds: Southwest (minLon, minLat), Northeast (maxLon, maxLat)
SELECT
    id,
    community_name,
    population,
    ST_X(coordinates::geometry) AS longitude,
    ST_Y(coordinates::geometry) AS latitude
FROM data_schema.community_data
WHERE coordinates && ST_MakeEnvelope(
    18.0,   -- minLon (west)
    -13.0,  -- minLat (south)
    19.0,   -- maxLon (east)
    -12.0,  -- maxLat (north)
    4326
)::geography;
```

**Performance Notes:**
- `&&` operator (overlaps) uses spatial index
- Very fast for map rendering
- Returns all points within rectangular bounds

---

### 5. Service Area (Voronoi Polygons)

**Use Case:** Determine which communities are served by which facilities

```sql
-- Create Voronoi diagram for infrastructure service areas
-- Note: Complex operation, better done in Python with Shapely or D3.js
-- Here's a simplified version using buffer zones

SELECT
    i.id AS infrastructure_id,
    i.facility_name,
    ST_Buffer(i.coordinates::geometry, 0.05)::geography AS service_area,  -- ~5km buffer
    ARRAY_AGG(c.community_name) AS communities_served
FROM data_schema.infrastructure_data i
LEFT JOIN data_schema.community_data c ON
    ST_DWithin(i.coordinates, c.coordinates, 5000)  -- 5km
GROUP BY i.id, i.facility_name, i.coordinates;
```

**Note:** True Voronoi diagram calculation is better done client-side with D3-Delaunay or server-side with Python Shapely.

---

### 6. Export as GeoJSON

**Use Case:** Export data for map visualization or download

```sql
-- Export communities as GeoJSON FeatureCollection
SELECT jsonb_build_object(
    'type', 'FeatureCollection',
    'features', jsonb_agg(feature)
) AS geojson
FROM (
    SELECT jsonb_build_object(
        'type', 'Feature',
        'id', id,
        'geometry', ST_AsGeoJSON(coordinates::geometry)::jsonb,
        'properties', jsonb_build_object(
            'name', community_name,
            'population', population,
            'region', region,
            'district', district
        )
    ) AS feature
    FROM data_schema.community_data
    WHERE uploaded_by = :user_id  -- Filter by user
) features;
```

**Output Example:**
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "geometry": {
        "type": "Point",
        "coordinates": [18.5053, -12.5047]
      },
      "properties": {
        "name": "Kalondama",
        "population": 2500,
        "region": "Luanda",
        "district": "Viana"
      }
    }
  ]
}
```

---

### 7. Risk Heatmap Data

**Use Case:** Generate heatmap intensity values for risk visualization

```sql
-- Get communities with risk scores for heatmap
SELECT
    c.id,
    c.community_name,
    ST_X(c.coordinates::geometry) AS longitude,
    ST_Y(c.coordinates::geometry) AS latitude,
    COALESCE(rr.risk_score, 50) AS risk_score,  -- Default to medium risk if no assessment
    COALESCE(rr.risk_score / 100.0, 0.5) AS intensity  -- Normalize to 0-1 for heatmap
FROM data_schema.community_data c
LEFT JOIN analysis_schema.risk_results rr ON
    c.id = rr.location_id
    AND rr.location_type = 'community'
WHERE c.uploaded_by = :user_id;
```

**Performance Notes:**
- Join with risk_results to get latest assessment
- Normalize risk_score (0-100) to intensity (0-1) for heatmap rendering
- COALESCE provides default values for communities without assessments

---

### 8. Spatial Join: Water Quality Near Communities

**Use Case:** Risk scoring - aggregate water quality measurements near community

```sql
-- Get water quality measurements within 2km of each community
SELECT
    c.id AS community_id,
    c.community_name,
    AVG(h.measurement_value) FILTER (WHERE h.parameter_name = 'arsenic') AS avg_arsenic,
    AVG(h.measurement_value) FILTER (WHERE h.parameter_name = 'nitrate') AS avg_nitrate,
    AVG(h.measurement_value) FILTER (WHERE h.parameter_name = 'e_coli') AS avg_e_coli,
    COUNT(DISTINCT h.id) AS measurement_count,
    MAX(h.measurement_date) AS latest_measurement_date
FROM data_schema.community_data c
LEFT JOIN data_schema.hydro_data h ON
    ST_DWithin(c.coordinates, h.coordinates, 2000)  -- 2km
    AND h.measurement_date > CURRENT_DATE - INTERVAL '1 year'  -- Last year only
GROUP BY c.id, c.community_name;
```

**Use in Risk Scoring:**
- Aggregates multiple measurements near community
- Filters by date to get recent data
- Used as input to risk_scoring.calculate_risk_score()

---

### 9. Calculate Population Pressure

**Use Case:** Population pressure risk component

```sql
-- Calculate people per functional water point by region
WITH functional_facilities AS (
    SELECT
        region,
        COUNT(*) AS functional_count
    FROM data_schema.infrastructure_data i
    JOIN data_schema.community_data c ON
        ST_DWithin(i.coordinates, c.coordinates, 10000)  -- Associate with region
    WHERE i.functionality = 'functional'
    GROUP BY region
),
regional_population AS (
    SELECT
        region,
        SUM(population) AS total_population
    FROM data_schema.community_data
    GROUP BY region
)
SELECT
    r.region,
    r.total_population,
    COALESCE(f.functional_count, 0) AS functional_facilities,
    CASE
        WHEN COALESCE(f.functional_count, 0) > 0
        THEN r.total_population / f.functional_count
        ELSE NULL
    END AS people_per_facility
FROM regional_population r
LEFT JOIN functional_facilities f ON r.region = f.region;
```

---

### 10. Cluster Analysis (for Map Marker Clustering)

**Use Case:** Group nearby communities for map display at low zoom levels

```sql
-- Cluster communities using ST_ClusterDBSCAN
-- Groups communities within 5km of each other
SELECT
    ST_ClusterDBSCAN(coordinates::geometry, eps := 0.05, minpoints := 2) OVER () AS cluster_id,
    id,
    community_name,
    ST_X(coordinates::geometry) AS longitude,
    ST_Y(coordinates::geometry) AS latitude,
    population
FROM data_schema.community_data;
```

**For Map Clustering:**
- `eps := 0.05` = ~5km clustering distance
- `minpoints := 2` = minimum 2 points per cluster
- Returns cluster_id for grouping markers

---

## Performance Optimization

### Index Types

```sql
-- GIST index for spatial operations (already created in schema)
CREATE INDEX idx_community_coordinates ON data_schema.community_data USING GIST (coordinates);

-- Additional indexes for common filters
CREATE INDEX idx_infrastructure_functionality ON data_schema.infrastructure_data (functionality);
CREATE INDEX idx_hydro_parameter ON data_schema.hydro_data (parameter_name);
CREATE INDEX idx_hydro_date ON data_schema.hydro_data (measurement_date DESC);
```

### Query Optimization Tips

1. **Use ST_DWithin over ST_Distance for filtering**
   ```sql
   -- GOOD (uses index)
   WHERE ST_DWithin(a.geom, b.geom, 10000)

   -- BAD (cannot use index)
   WHERE ST_Distance(a.geom, b.geom) < 10000
   ```

2. **Use <-> operator for nearest neighbor queries**
   ```sql
   -- GOOD (KNN-GIST index)
   ORDER BY a.geom <-> b.geom LIMIT 1

   -- BAD (full table scan)
   ORDER BY ST_Distance(a.geom, b.geom) LIMIT 1
   ```

3. **Cast GEOGRAPHY to GEOMETRY for visualization**
   ```sql
   -- GEOGRAPHY for distance calculations (meters)
   SELECT ST_Distance(a.coordinates, b.coordinates) FROM ...

   -- GEOMETRY for coordinate extraction (degrees)
   SELECT ST_X(a.coordinates::geometry), ST_Y(a.coordinates::geometry) FROM ...
   ```

4. **Use bounding box filter before expensive operations**
   ```sql
   -- First filter with bounding box (fast), then precise distance (slower)
   WHERE coordinates && ST_MakeEnvelope(...)  -- Bounding box check (fast)
     AND ST_DWithin(coordinates, point, radius)  -- Precise distance (slower)
   ```

---

## Common Spatial Functions Reference

### Distance Functions

| Function | Description | Returns |
|----------|-------------|---------|
| `ST_Distance(geog1, geog2)` | Distance in meters (GEOGRAPHY) | double |
| `ST_DWithin(geog1, geog2, distance)` | Within distance? (uses index) | boolean |
| `geom1 <-> geom2` | Distance operator for KNN | double |

### Geometry Creation

| Function | Description | Returns |
|----------|-------------|---------|
| `ST_MakePoint(lon, lat)` | Create point | geometry |
| `ST_SetSRID(geom, srid)` | Set coordinate system | geometry |
| `ST_MakeEnvelope(minX, minY, maxX, maxY, srid)` | Create bounding box | geometry |

### Geometry Conversion

| Function | Description | Returns |
|----------|-------------|---------|
| `geog::geometry` | Convert GEOGRAPHY to GEOMETRY | geometry |
| `geom::geography` | Convert GEOMETRY to GEOGRAPHY | geography |
| `ST_AsGeoJSON(geom)` | Export as GeoJSON | text |
| `ST_X(geom)` | Extract longitude | double |
| `ST_Y(geom)` | Extract latitude | double |

### Spatial Relationships

| Function | Description | Returns |
|----------|-------------|---------|
| `ST_Intersects(geom1, geom2)` | Geometries intersect? | boolean |
| `ST_Within(geom1, geom2)` | geom1 within geom2? | boolean |
| `geom1 && geom2` | Bounding boxes overlap? (fast) | boolean |

### Geometry Processing

| Function | Description | Returns |
|----------|-------------|---------|
| `ST_Buffer(geog, radius)` | Create buffer (meters) | geography |
| `ST_Transform(geom, target_srid)` | Change coordinate system | geometry |
| `ST_ClusterDBSCAN(geom, eps, minpoints)` | Cluster points | int (cluster ID) |

---

## Example API Integration (FastAPI)

### Find Nearby Communities Endpoint

```python
# Future: data-service/api/map_routes.py
from fastapi import APIRouter, Query, Depends
from sqlalchemy import select, func, text
from geoalchemy2.functions import ST_DWithin, ST_Distance, ST_X, ST_Y

router = APIRouter(prefix="/v1/map", tags=["map"])

@router.get("/communities/nearby")
async def get_nearby_communities(
    latitude: float = Query(..., ge=-90, le=90),
    longitude: float = Query(..., ge=-180, le=180),
    radius_km: float = Query(10, ge=0, le=100),
    user_id: str = Depends(get_current_user_id),
    db: Session = Depends(get_db)
):
    """
    Find communities within radius_km of a point.
    """
    # Create point
    point = func.ST_SetSRID(func.ST_MakePoint(longitude, latitude), 4326)

    # Query
    query = select(
        CommunityData.id,
        CommunityData.community_name,
        CommunityData.population,
        ST_X(CommunityData.coordinates.cast(Geometry)).label('longitude'),
        ST_Y(CommunityData.coordinates.cast(Geometry)).label('latitude'),
        (func.ST_Distance(CommunityData.coordinates, point.cast(Geography)) / 1000).label('distance_km')
    ).where(
        CommunityData.uploaded_by == user_id,
        ST_DWithin(
            CommunityData.coordinates,
            point.cast(Geography),
            radius_km * 1000  # Convert to meters
        )
    ).order_by(text('distance_km'))

    results = await db.execute(query)

    return {
        "type": "FeatureCollection",
        "features": [
            {
                "type": "Feature",
                "geometry": {
                    "type": "Point",
                    "coordinates": [row.longitude, row.latitude]
                },
                "properties": {
                    "id": str(row.id),
                    "name": row.community_name,
                    "population": row.population,
                    "distance_km": round(row.distance_km, 2)
                }
            }
            for row in results
        ]
    }
```

---

## Testing Spatial Queries

### Sample Test Data

```sql
-- Insert test communities
INSERT INTO data_schema.community_data (community_name, latitude, longitude, coordinates, population, uploaded_by)
VALUES
    ('Kalondama', -12.5047, 18.5053, ST_SetSRID(ST_MakePoint(18.5053, -12.5047), 4326)::geography, 2500, '550e8400-e29b-41d4-a716-446655440000'),
    ('Vila Verde', -12.5147, 18.5153, ST_SetSRID(ST_MakePoint(18.5153, -12.5147), 4326)::geography, 1800, '550e8400-e29b-41d4-a716-446655440000'),
    ('Cacuaco', -8.7832, 13.3719, ST_SetSRID(ST_MakePoint(13.3719, -8.7832), 4326)::geography, 3200, '550e8400-e29b-41d4-a716-446655440000');

-- Insert test infrastructure
INSERT INTO data_schema.infrastructure_data (facility_name, facility_type, latitude, longitude, coordinates, functionality, uploaded_by)
VALUES
    ('Borehole 1', 'borehole', -12.5050, 18.5055, ST_SetSRID(ST_MakePoint(18.5055, -12.5050), 4326)::geography, 'functional', '550e8400-e29b-41d4-a716-446655440000'),
    ('Well 2', 'protected_well', -12.5140, 18.5150, ST_SetSRID(ST_MakePoint(18.5150, -12.5140), 4326)::geography, 'needs_repair', '550e8400-e29b-41d4-a716-446655440000');
```

### Verify Distance Calculations

```sql
-- Test: Distance between Kalondama and Borehole 1 (should be ~0.4km)
SELECT
    ST_Distance(
        ST_SetSRID(ST_MakePoint(18.5053, -12.5047), 4326)::geography,  -- Kalondama
        ST_SetSRID(ST_MakePoint(18.5055, -12.5050), 4326)::geography   -- Borehole 1
    ) / 1000 AS distance_km;
-- Expected: ~0.4 km

-- Test: Find communities within 10km of point
SELECT community_name, ST_Distance(coordinates, ST_SetSRID(ST_MakePoint(18.5053, -12.5047), 4326)::geography) / 1000 AS distance_km
FROM data_schema.community_data
WHERE ST_DWithin(coordinates, ST_SetSRID(ST_MakePoint(18.5053, -12.5047), 4326)::geography, 10000)
ORDER BY distance_km;
```

---

## Troubleshooting

### Issue 1: Slow spatial queries

**Solution:** Ensure GIST indexes exist
```sql
-- Check indexes
SELECT schemaname, tablename, indexname
FROM pg_indexes
WHERE indexname LIKE '%coordinate%';

-- Create if missing
CREATE INDEX idx_community_coordinates ON data_schema.community_data USING GIST (coordinates);
```

### Issue 2: Distance calculations return unexpected values

**Solution:** Verify SRID and GEOGRAPHY vs GEOMETRY
```sql
-- Check SRID
SELECT ST_SRID(coordinates) FROM data_schema.community_data LIMIT 1;
-- Should return: 4326

-- GEOGRAPHY (spherical earth, meters) vs GEOMETRY (planar, degrees)
SELECT
    ST_Distance(a.coordinates, b.coordinates) AS geog_distance_meters,  -- Correct
    ST_Distance(a.coordinates::geometry, b.coordinates::geometry) AS geom_distance_degrees  -- Wrong for distance
FROM data_schema.community_data a, data_schema.community_data b
WHERE a.id != b.id
LIMIT 1;
```

### Issue 3: Coordinate order confusion

**PostGIS Convention:** `ST_MakePoint(longitude, latitude)`
**GeoJSON Convention:** `[longitude, latitude]`

```sql
-- CORRECT: longitude first, latitude second
ST_MakePoint(18.5053, -12.5047)  -- (lon, lat)

-- INCORRECT (swapped):
ST_MakePoint(-12.5047, 18.5053)  -- (lat, lon) - WRONG!
```

---

## References

- **PostGIS Documentation**: https://postgis.net/documentation/
- **Database Schema**: database/postgres/V1_SCHEMA_MVP.sql
- **Agent 05**: GIS_3D_VISUALIZATION.md (detailed spatial patterns)
- **Risk Scoring**: ai-model/risk_scoring.py (uses distance calculations)

**Last Updated**: 2026-02-03
**Next Review**: After frontend map implementation
