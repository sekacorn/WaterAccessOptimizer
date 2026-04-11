# API Contracts - MVP Specification
> Status: reference documentation. Validate routes, ports, and packaged service paths against [ARCHITECTURE_OVERVIEW.md](./ARCHITECTURE_OVERVIEW.md) and [../README.md](../README.md) before treating this as release truth.

**Version:** 1.0.0
**Last Updated:** 2026-02-03
**Status:** MVP (Iteration 7)
**Reference:** Agent 09 (API_CONTRACTS_TYPES.md)

---

## Overview

This document defines the API contracts for MVP services. OpenAPI-first approach ensures type safety, auto-generated documentation, and contract testing.

**Key Principles:**
- RESTful for CRUD operations
- JSON for all request/response bodies
- Consistent error format across all services
- Bearer token (JWT) authentication
- Request ID tracking for debugging

---

## MVP Services & Endpoints

### Auth Service (Port 8086)

Base URL: `http://localhost:8086/v1/auth`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/register` | Register new user | No |
| POST | `/login` | Login user (get JWT) | No |
| POST | `/logout` | Logout user | Yes |
| GET | `/me` | Get current user profile | Yes |

### Worker Service (Future: Port 8090)

Base URL: `http://localhost:8090/v1/risk`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/assessments` | Calculate risk assessment | Yes |
| GET | `/assessments` | List user's assessments | Yes |
| GET | `/assessments/{id}` | Get assessment details | Yes |
| GET | `/assessments/{id}/summary` | Get markdown summary | Yes |

### Data Service (Future: Port 8088)

Base URL: `http://localhost:8088/v1/data`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| POST | `/upload/hydro` | Upload water quality data (CSV/GeoJSON) | Yes |
| POST | `/upload/community` | Upload community data (CSV/GeoJSON) | Yes |
| POST | `/upload/infrastructure` | Upload infrastructure data (CSV/GeoJSON) | Yes |
| GET | `/datasets` | List user's datasets | Yes |
| GET | `/datasets/{id}` | Get dataset details + validation errors | Yes |
| DELETE | `/datasets/{id}` | Delete dataset | Yes |

### Map Service (Future: Port 8089)

Base URL: `http://localhost:8089/v1/map`

| Method | Endpoint | Description | Auth Required |
|--------|----------|-------------|---------------|
| GET | `/communities` | Get communities with coordinates | Yes |
| GET | `/communities/nearby` | Find communities within radius | Yes |
| GET | `/infrastructure` | Get infrastructure with coordinates | Yes |
| GET | `/risk-heatmap` | Get risk heatmap data (GeoJSON) | Yes |
| GET | `/export/geojson` | Export map data as GeoJSON | Yes |

---

## Type Definitions

### Auth Types

```typescript
// Auth Request/Response Types

export interface RegisterRequest {
  email: string;                    // Format: email, Required
  password: string;                 // Min length: 8, Required
  full_name: string;                // Min: 2, Max: 100, Required
  organization_name?: string;       // Optional
}

export interface LoginRequest {
  email: string;                    // Format: email, Required
  password: string;                 // Required
}

export interface AuthResponse {
  access_token: string;             // JWT token (24h expiry for MVP)
  refresh_token: string;            // Refresh token (7 days, Sprint 2)
  token_type: 'bearer';             // Always 'bearer'
  expires_in: number;               // Seconds until expiration
  user: User;                       // User profile
}

export interface User {
  id: string;                       // UUID
  email: string;                    // User email
  full_name: string;                // Display name
  role: 'USER' | 'ADMIN';          // MVP roles only
  organization_id: string | null;   // UUID or null
  organization_name: string | null; // Org name or null
  storage_quota_bytes: number;      // Storage quota (100MB MVP)
  storage_used_bytes: number;       // Current usage
  created_at: string;               // ISO 8601 timestamp
}
```

### Data Types

```typescript
// Community Data Types

export interface Community {
  id: string;                       // UUID
  community_name: string;           // Community name
  latitude: number;                 // -90 to 90
  longitude: number;                // -180 to 180
  population: number;               // > 0
  region: string | null;            // Optional
  district: string | null;          // Optional
  elevation_m: number | null;       // Elevation in meters
  created_at: string;               // ISO 8601
  updated_at: string;               // ISO 8601
}

// Infrastructure Data Types

export interface Infrastructure {
  id: string;                       // UUID
  facility_name: string;            // Facility name
  facility_type: 'borehole' | 'protected_well' | 'unprotected_well' | 'surface_water' | 'piped_network';
  latitude: number;                 // -90 to 90
  longitude: number;                // -180 to 180
  functionality: 'functional' | 'needs_repair' | 'non_functional';
  installation_date: string | null; // ISO 8601 date
  last_maintenance: string | null;  // ISO 8601 date
  capacity_l_per_day: number | null; // Liters per day
  population_served: number | null; // People served
  created_at: string;               // ISO 8601
  updated_at: string;               // ISO 8601
}

// Water Quality Data Types

export interface WaterQualityMeasurement {
  id: string;                       // UUID
  location_name: string;            // Measurement location
  latitude: number;                 // -90 to 90
  longitude: number;                // -180 to 180
  measurement_date: string;         // ISO 8601 date
  parameter_name: 'arsenic' | 'fluoride' | 'nitrate' | 'lead' | 'mercury' | 'chromium' | 'e_coli' | 'total_coliform' | 'turbidity' | 'ph';
  measurement_value: number;        // Measured value
  measurement_unit: string;         // Unit (mg/L, CFU/100mL, NTU, pH)
  source: 'field_survey' | 'lab_analysis' | 'usgs' | 'other';
  data_quality_score: number;       // 0.0 to 1.0
  created_at: string;               // ISO 8601
}

// Upload Response Types

export interface UploadResponse {
  dataset_id: string;               // UUID of uploaded dataset
  filename: string;                 // Original filename
  dataset_type: 'hydro' | 'community' | 'infrastructure';
  records_imported: number;         // Successfully imported records
  records_failed: number;           // Failed records
  validation_status: 'VALID' | 'WARNINGS' | 'ERRORS';
  validation_warnings: ValidationIssue[];
  validation_errors: ValidationIssue[];
  upload_size_bytes: number;        // File size
  created_at: string;               // Upload timestamp
}

export interface ValidationIssue {
  row: number;                      // Row number (1-indexed)
  column: string;                   // Column name
  value: string | null;             // Invalid value
  error_code: string;               // Machine-readable code
  message: string;                  // Human-readable message
  severity: 'ERROR' | 'WARNING';    // Severity level
}
```

### Risk Assessment Types

```typescript
// Risk Assessment Request/Response

export interface AssessmentRequest {
  name: string;                     // Assessment name
  water_quality_dataset_ids: string[]; // UUID array
  community_dataset_ids: string[];  // UUID array
  infrastructure_dataset_ids: string[]; // UUID array
  parameters_to_check?: string[];   // Optional: specific WHO parameters
}

export interface AssessmentResponse {
  assessment_id: string;            // UUID
  name: string;                     // Assessment name
  status: 'PENDING' | 'RUNNING' | 'COMPLETED' | 'FAILED';
  algorithm_version: string;        // e.g., "1.0.0"
  created_at: string;               // ISO 8601
  completed_at: string | null;      // ISO 8601 or null
  overall_risk_score: number;       // 0-100 (when completed)
  risk_level: 'LOW' | 'MEDIUM' | 'HIGH' | null; // When completed
  confidence_level: 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH' | null;
  sample_size: number;              // Total data points used
  component_scores: ComponentScores | null;
  top_factors: RiskFactor[] | null; // Top 3 factors
  error_message: string | null;     // Error if failed
}

export interface ComponentScores {
  water_quality: number;            // 0-100
  access_distance: number;          // 0-100
  infrastructure: number;           // 0-100
  population_pressure: number;      // 0-100
}

export interface RiskFactor {
  component: 'water_quality' | 'access_distance' | 'infrastructure' | 'population_pressure';
  measured_value: number;           // Actual measured value
  guideline_value: number | string; // WHO guideline (or range for pH)
  impact_description: string;       // Health impact explanation
  contribution_percent: number;     // % contribution to overall risk
  severity: 'HIGH' | 'MEDIUM' | 'LOW';
}

// Summary Response

export interface AssessmentSummary {
  full_text: string;                // Full markdown summary
  short_text: string;               // Short summary (<100 words)
  json_summary: JsonSummary;        // Structured JSON
}

export interface JsonSummary {
  overall_risk: {
    score: number;                  // 0-100
    level: 'LOW' | 'MEDIUM' | 'HIGH';
    description: string;            // Risk level description
  };
  confidence: {
    level: 'NONE' | 'LOW' | 'MEDIUM' | 'HIGH';
    sample_size: number;            // Data points used
    description: string;            // Confidence description
  };
  top_factors: RiskFactor[];        // Top 3 factors
  component_scores: ComponentScores; // 4 component scores
  metadata: {
    algorithm_version: string;      // e.g., "1.0.0"
    generated_at: string;           // ISO 8601
    sample_size: number;            // Total samples
  };
}
```

### Map/Spatial Types

```typescript
// Map Query Types

export interface NearbyCommunitiesQuery {
  latitude: number;                 // Center point latitude
  longitude: number;                // Center point longitude
  radius_km: number;                // Radius in kilometers (max 100)
}

export interface BoundingBoxQuery {
  min_lat: number;                  // Southwest latitude
  min_lon: number;                  // Southwest longitude
  max_lat: number;                  // Northeast latitude
  max_lon: number;                  // Northeast longitude
}

// GeoJSON Response Types

export interface GeoJSONFeatureCollection {
  type: 'FeatureCollection';
  features: GeoJSONFeature[];
}

export interface GeoJSONFeature {
  type: 'Feature';
  geometry: {
    type: 'Point' | 'LineString' | 'Polygon';
    coordinates: number[] | number[][] | number[][][]; // Depends on geometry type
  };
  properties: {
    id: string;                     // Entity UUID
    type: 'community' | 'infrastructure' | 'measurement';
    name: string;                   // Display name
    [key: string]: any;             // Additional properties
  };
}

// Risk Heatmap Data

export interface RiskHeatmapData {
  type: 'FeatureCollection';
  features: RiskHeatmapFeature[];
}

export interface RiskHeatmapFeature {
  type: 'Feature';
  geometry: {
    type: 'Point';
    coordinates: [number, number]; // [longitude, latitude]
  };
  properties: {
    community_id: string;           // UUID
    community_name: string;         // Name
    risk_score: number;             // 0-100
    risk_level: 'LOW' | 'MEDIUM' | 'HIGH';
    intensity: number;              // 0-1 (for heatmap rendering)
  };
}
```

### Error Types

```typescript
// Standard Error Response

export interface ErrorResponse {
  error: string;                    // Machine-readable error code
  message: string;                  // Human-readable message
  details?: any;                    // Additional details (optional)
  timestamp: string;                // ISO 8601
  request_id?: string;              // Request ID for debugging
}

// Validation Error Response

export interface ValidationErrorResponse extends ErrorResponse {
  validation_errors: ValidationIssue[];
}

// Common Error Codes

export enum ErrorCode {
  INVALID_REQUEST = 'INVALID_REQUEST',
  VALIDATION_ERROR = 'VALIDATION_ERROR',
  UNAUTHORIZED = 'UNAUTHORIZED',
  INVALID_CREDENTIALS = 'INVALID_CREDENTIALS',
  FORBIDDEN = 'FORBIDDEN',
  NOT_FOUND = 'NOT_FOUND',
  CONFLICT = 'CONFLICT',
  PAYLOAD_TOO_LARGE = 'PAYLOAD_TOO_LARGE',
  RATE_LIMIT_EXCEEDED = 'RATE_LIMIT_EXCEEDED',
  INTERNAL_SERVER_ERROR = 'INTERNAL_SERVER_ERROR'
}
```

---

## Example API Calls

### 1. Register User

**Request:**
```http
POST /v1/auth/register HTTP/1.1
Host: localhost:8086
Content-Type: application/json

{
  "email": "jane@wateraid.org",
  "password": "SecurePass123!",
  "full_name": "Jane Doe",
  "organization_name": "WaterAid Angola"
}
```

**Response (201 Created):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiI1NTBlODQwMC1lMjliLTQxZDQtYTcxNi00NDY2NTU0NDAwMDAiLCJlbWFpbCI6ImphbmVAd2F0ZXJhaWQub3JnIiwicm9sZSI6IlVTRVIiLCJ0eXBlIjoiYWNjZXNzIiwiaWF0IjoxNzA1NzU0NDAwLCJleHAiOjE3MDU4NDA4MDB9.signature",
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "expires_in": 86400,
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "jane@wateraid.org",
    "full_name": "Jane Doe",
    "role": "USER",
    "organization_id": "550e8400-e29b-41d4-a716-446655440001",
    "organization_name": "WaterAid Angola",
    "storage_quota_bytes": 104857600,
    "storage_used_bytes": 0,
    "created_at": "2026-02-03T08:00:00Z"
  }
}
```

### 2. Upload Community Data

**Request:**
```http
POST /v1/data/upload/community HTTP/1.1
Host: localhost:8088
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: multipart/form-data; boundary=----WebKitFormBoundary

------WebKitFormBoundary
Content-Disposition: form-data; name="file"; filename="communities.csv"
Content-Type: text/csv

community_name,latitude,longitude,population,region,district
Kalondama,-12.5047,18.5053,2500,Luanda,Viana
Cacuaco,-8.7832,13.3719,3200,Luanda,Cacuaco
------WebKitFormBoundary--
```

**Response (201 Created):**
```json
{
  "dataset_id": "550e8400-e29b-41d4-a716-446655440002",
  "filename": "communities.csv",
  "dataset_type": "community",
  "records_imported": 2,
  "records_failed": 0,
  "validation_status": "VALID",
  "validation_warnings": [],
  "validation_errors": [],
  "upload_size_bytes": 256,
  "created_at": "2026-02-03T08:05:00Z"
}
```

### 3. Calculate Risk Assessment

**Request:**
```http
POST /v1/risk/assessments HTTP/1.1
Host: localhost:8090
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
Content-Type: application/json

{
  "name": "Luanda Region Assessment - Feb 2026",
  "water_quality_dataset_ids": ["550e8400-e29b-41d4-a716-446655440003"],
  "community_dataset_ids": ["550e8400-e29b-41d4-a716-446655440002"],
  "infrastructure_dataset_ids": ["550e8400-e29b-41d4-a716-446655440004"],
  "parameters_to_check": ["arsenic", "nitrate", "e_coli"]
}
```

**Response (200 OK):**
```json
{
  "assessment_id": "550e8400-e29b-41d4-a716-446655440005",
  "name": "Luanda Region Assessment - Feb 2026",
  "status": "COMPLETED",
  "algorithm_version": "1.0.0",
  "created_at": "2026-02-03T08:10:00Z",
  "completed_at": "2026-02-03T08:10:05Z",
  "overall_risk_score": 26.4,
  "risk_level": "LOW",
  "confidence_level": "LOW",
  "sample_size": 6,
  "component_scores": {
    "water_quality": 25.2,
    "access_distance": 15.1,
    "infrastructure": 25.0,
    "population_pressure": 68.0
  },
  "top_factors": [
    {
      "component": "water_quality",
      "measured_value": 5.0,
      "guideline_value": 0,
      "impact_description": "Indicates fecal contamination, causes diarrheal disease",
      "contribution_percent": 35.0,
      "severity": "HIGH"
    },
    {
      "component": "water_quality",
      "measured_value": 2.0,
      "guideline_value": 0,
      "impact_description": "Indicates fecal contamination, causes diarrheal disease",
      "contribution_percent": 14.0,
      "severity": "HIGH"
    },
    {
      "component": "water_quality",
      "measured_value": 0.03,
      "guideline_value": 0.01,
      "impact_description": "Chronic exposure causes cancer, skin lesions, cardiovascular disease",
      "contribution_percent": 14.0,
      "severity": "HIGH"
    }
  ],
  "error_message": null
}
```

### 4. Get Assessment Summary

**Request:**
```http
GET /v1/risk/assessments/550e8400-e29b-41d4-a716-446655440005/summary?format=json HTTP/1.1
Host: localhost:8090
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**
```json
{
  "overall_risk": {
    "score": 26.4,
    "level": "LOW",
    "description": "Water access conditions are generally acceptable"
  },
  "confidence": {
    "level": "LOW",
    "sample_size": 6,
    "description": "Limited data, preliminary indication only"
  },
  "top_factors": [...],
  "component_scores": {
    "water_quality": 25.2,
    "access_distance": 15.1,
    "infrastructure": 25.0,
    "population_pressure": 68.0
  },
  "metadata": {
    "algorithm_version": "1.0.0",
    "generated_at": "2026-02-03T08:10:05Z",
    "sample_size": 6
  }
}
```

### 5. Find Nearby Communities

**Request:**
```http
GET /v1/map/communities/nearby?latitude=-12.5047&longitude=18.5053&radius_km=10 HTTP/1.1
Host: localhost:8089
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**
```json
{
  "type": "FeatureCollection",
  "features": [
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [18.5053, -12.5047]
      },
      "properties": {
        "id": "550e8400-e29b-41d4-a716-446655440006",
        "type": "community",
        "name": "Kalondama",
        "population": 2500,
        "distance_km": 0.0
      }
    },
    {
      "type": "Feature",
      "geometry": {
        "type": "Point",
        "coordinates": [18.5153, -12.5147]
      },
      "properties": {
        "id": "550e8400-e29b-41d4-a716-446655440007",
        "type": "community",
        "name": "Vila Verde",
        "population": 1800,
        "distance_km": 1.2
      }
    }
  ]
}
```

---

## Common HTTP Status Codes

| Status | Meaning | When to Use |
|--------|---------|-------------|
| 200 | OK | Successful GET, PUT, DELETE |
| 201 | Created | Successful POST (created resource) |
| 204 | No Content | Successful DELETE (no body returned) |
| 400 | Bad Request | Invalid request format, validation errors |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | User lacks required role (e.g., ADMIN only) |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Resource already exists (e.g., duplicate email) |
| 413 | Payload Too Large | Upload exceeds 10MB limit |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Unexpected server error |

---

## Authentication Flow

### JWT Token Format

**Access Token Claims:**
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "jane@wateraid.org",
  "role": "USER",
  "type": "access",
  "iat": 1705754400,
  "exp": 1705840800
}
```

**Refresh Token Claims:**
```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "type": "refresh",
  "iat": 1705754400,
  "exp": 1706359200
}
```

### Authorization Header

All authenticated endpoints require:
```http
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Token Refresh Flow (Sprint 2)

**Request:**
```http
POST /v1/auth/refresh HTTP/1.1
Content-Type: application/json

{
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

**Response (200 OK):**
```json
{
  "access_token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "bearer",
  "expires_in": 86400
}
```

---

## Rate Limiting (Sprint 2+)

**MVP:** No rate limiting
**V1:** 100 requests/minute per user

**Rate Limit Headers:**
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 97
X-RateLimit-Reset: 1705755000
```

**429 Response:**
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Too many requests. Please try again in 60 seconds.",
  "timestamp": "2026-02-03T08:15:00Z",
  "request_id": "req-abc123"
}
```

---

## Request ID Tracking

All responses include `X-Request-Id` header for debugging:
```http
HTTP/1.1 200 OK
X-Request-Id: req-abc123
Content-Type: application/json
```

Log format:
```
2026-02-03T08:10:00Z [req-abc123] POST /v1/risk/assessments 200 OK 5.2s
```

---

## Implementation Checklist

### MVP (Sprint 1)
- [ ] Define TypeScript types for all MVP endpoints
- [ ] Implement standard error response format
- [ ] Add JWT authentication middleware
- [ ] Add request ID tracking
- [ ] Create Postman collection for testing
- [ ] Generate API documentation (Swagger UI)

### V1 (Sprint 2)
- [ ] Add refresh token rotation
- [ ] Implement rate limiting
- [ ] Add pagination to list endpoints
- [ ] Create OpenAPI 3.1 specifications
- [ ] Auto-generate client types

---

## References

- **Agent 09**: API_CONTRACTS_TYPES.md (full specifications)
- **Iteration 5**: risk_scoring.py (algorithm implementation)
- **Iteration 6**: risk_summary_generator.py (summary generation)
- **Database Schema**: database/postgres/V1_SCHEMA_MVP.sql

**Last Updated**: 2026-02-03
**Next Review**: After frontend implementation (Iteration 8)
