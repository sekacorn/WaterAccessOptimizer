# Service Contracts

> Status: historical/reference contract document. Validate every route, service name, and port against the current release docs before implementation or release use.

**Water Access Optimizer - MVP Service API Contracts**

Version: 0.1.0 (MVP)
Last Updated: January 26, 2024

---

## Table of Contents

1. [Overview](#overview)
2. [Authentication & Authorization](#authentication--authorization)
3. [Service 1: API Gateway](#service-1-api-gateway)
4. [Service 2: Auth Service](#service-2-auth-service)
5. [Service 3: Data Service](#service-3-data-service)
6. [Service 4: Worker Service](#service-4-worker-service)
7. [Inter-Service Communication](#inter-service-communication)
8. [Error Handling](#error-handling)
9. [Rate Limiting](#rate-limiting)
10. [Data Schemas](#data-schemas)

---

## Overview

This document defines the API contracts for all microservices in the Water Access Optimizer MVP (v0.1.0). All APIs follow RESTful conventions with JSON request/response bodies.

### Base URLs (Development)

- **API Gateway**: `http://localhost:8080` (all client requests go through here)
- **Auth Service**: `http://localhost:8086` (internal only, not exposed)
- **Data Service**: `http://localhost:8082` (internal only, not exposed)
- **Worker Service**: `http://localhost:8000` (internal only, not exposed)

### Common Headers

**All authenticated requests must include**:
```http
Authorization: Bearer {jwt_token}
Content-Type: application/json
```

**All responses include**:
```http
Content-Type: application/json
X-Request-Id: {uuid}
X-RateLimit-Remaining: {count}
```

---

## Authentication & Authorization

### JWT Token Structure

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1706000000,
  "exp": 1706086400
}
```

**Token Expiration**: 24 hours from issue time

### Role-Based Access Control

| Role | Permissions |
|------|-------------|
| **USER** | Upload data, run analyses, view own resources |
| **ADMIN** | All USER permissions + user management + system stats |

### HTTP Status Codes

| Code | Meaning | When Used |
|------|---------|-----------|
| 200 | OK | Successful request |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid input, validation errors |
| 401 | Unauthorized | Missing or invalid JWT token |
| 403 | Forbidden | Insufficient permissions (role check failed) |
| 404 | Not Found | Resource doesn't exist |
| 409 | Conflict | Duplicate resource (e.g., email already exists) |
| 413 | Payload Too Large | File size exceeds limit |
| 429 | Too Many Requests | Rate limit exceeded |
| 500 | Internal Server Error | Server error |
| 503 | Service Unavailable | Downstream service unavailable |

---

## Service 1: API Gateway

**Technology**: Spring Cloud Gateway
**Port**: 8080
**Responsibilities**: Request routing, JWT validation, rate limiting, CORS

### Route Configuration

All client requests go through API Gateway. It validates JWTs and routes to backend services.

#### Public Routes (No Authentication)

```
POST   /api/auth/register       → Auth Service
POST   /api/auth/login          → Auth Service
GET    /actuator/health         → API Gateway (health check)
```

#### Protected Routes (JWT Required, USER role)

```
GET    /api/auth/me                       → Auth Service
PUT    /api/auth/me                       → Auth Service
POST   /api/auth/change-password          → Auth Service

POST   /api/data/upload/hydro             → Data Service
POST   /api/data/upload/community         → Data Service
POST   /api/data/upload/infrastructure    → Data Service
GET    /api/data/datasets                 → Data Service
GET    /api/data/datasets/{id}            → Data Service
DELETE /api/data/datasets/{id}            → Data Service
GET    /api/data/storage                  → Data Service

POST   /api/analysis/risk-score           → Worker Service
GET    /api/analysis/{id}                 → Worker Service
GET    /api/analysis/{id}/export/csv      → Worker Service
GET    /api/analysis                      → Worker Service
```

#### Admin Routes (JWT Required, ADMIN role)

```
GET    /api/admin/users                   → Auth Service
POST   /api/admin/users/{id}/deactivate   → Auth Service
POST   /api/admin/users/{id}/activate     → Auth Service
PUT    /api/admin/users/{id}/role         → Auth Service
GET    /api/admin/stats                   → Data Service
```

### JWT Validation & Header Propagation

After validating JWT, API Gateway adds headers for downstream services:

```http
X-User-Id: 550e8400-e29b-41d4-a716-446655440000
X-User-Email: user@example.com
X-User-Role: USER
X-Request-Id: 7a8b9c0d-1234-5678-9abc-def012345678
```

---

## Service 2: Auth Service

**Technology**: Spring Boot 3.2, Spring Security 6
**Port**: 8086 (internal only)
**Responsibilities**: User authentication, JWT generation, user management

### Endpoints

#### POST /auth/register

Register a new user account.

**Access**: Public (no authentication required)

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123",
  "firstName": "John",
  "lastName": "Doe",
  "organization": "Water NGO"
}
```

**Validations**:
- `email`: Valid email format, unique (not already registered)
- `password`: Min 8 characters, 1 uppercase, 1 lowercase, 1 number
- `firstName`, `lastName`: Min 2 characters, max 100 characters
- `organization`: Optional, max 255 characters

**Response** (201 Created):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "organization": "Water NGO",
    "role": "USER",
    "storageQuotaMb": 100,
    "createdAt": "2024-01-20T10:30:00Z"
  }
}
```

**Errors**:
- `400`: Invalid input (e.g., password too weak, email invalid format)
- `409`: Email already registered

**Error Response Example**:
```json
{
  "error": "VALIDATION_ERROR",
  "message": "Password must be at least 8 characters and contain 1 uppercase, 1 lowercase, 1 number",
  "field": "password",
  "timestamp": "2024-01-20T10:30:00Z",
  "requestId": "7a8b9c0d-1234-5678-9abc-def012345678"
}
```

---

#### POST /auth/login

Authenticate user and receive JWT token.

**Access**: Public (no authentication required)

**Request**:
```json
{
  "email": "user@example.com",
  "password": "SecurePass123"
}
```

**Response** (200 OK):
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "user": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "email": "user@example.com",
    "firstName": "John",
    "lastName": "Doe",
    "role": "USER",
    "lastLogin": "2024-01-20T10:30:00Z"
  }
}
```

**Errors**:
- `400`: Missing email or password
- `401`: Invalid credentials
- `403`: Account locked (5 failed attempts, 30-minute cooldown)

**Error Response Example (Account Locked)**:
```json
{
  "error": "ACCOUNT_LOCKED",
  "message": "Account locked due to multiple failed login attempts. Try again in 25 minutes.",
  "lockedUntil": "2024-01-20T11:00:00Z",
  "timestamp": "2024-01-20T10:35:00Z",
  "requestId": "7a8b9c0d-1234-5678-9abc-def012345678"
}
```

**Audit Logging**:
All login attempts (success/failure) are logged with:
- User ID (if email exists)
- IP address
- User agent
- Timestamp
- Result (success/failure/account_locked)

---

#### GET /auth/me

Get current user's profile.

**Access**: Authenticated (USER role)

**Request Headers**:
```http
Authorization: Bearer {token}
```

**Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "John",
  "lastName": "Doe",
  "organization": "Water NGO",
  "role": "USER",
  "storageQuotaMb": 100,
  "storageUsedMb": 12.3,
  "createdAt": "2024-01-20T10:30:00Z",
  "lastLogin": "2024-01-21T09:15:00Z"
}
```

**Errors**:
- `401`: Invalid or expired token

---

#### PUT /auth/me

Update current user's profile.

**Access**: Authenticated (USER role)

**Request**:
```json
{
  "firstName": "Jane",
  "lastName": "Smith",
  "organization": "Global Water Initiative"
}
```

**Response** (200 OK):
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "firstName": "Jane",
  "lastName": "Smith",
  "organization": "Global Water Initiative",
  "role": "USER",
  "updatedAt": "2024-01-21T10:00:00Z"
}
```

**Errors**:
- `400`: Invalid input
- `401`: Unauthorized

**Note**: Email and password cannot be updated via this endpoint (use dedicated endpoints)

---

#### POST /auth/change-password

Change user's password.

**Access**: Authenticated (USER role)

**Request**:
```json
{
  "currentPassword": "OldPass123",
  "newPassword": "NewSecurePass456"
}
```

**Response** (200 OK):
```json
{
  "message": "Password changed successfully. Please log in again with your new password.",
  "timestamp": "2024-01-21T10:30:00Z"
}
```

**Errors**:
- `400`: New password doesn't meet requirements
- `401`: Current password incorrect
- `422`: New password same as current password

---

#### GET /admin/users

List all users (admin only).

**Access**: Authenticated (ADMIN role required)

**Query Parameters**:
- `page`: Page number (default: 1)
- `limit`: Items per page (default: 20, max: 100)
- `role`: Filter by role (optional)
- `isActive`: Filter by active status (optional)

**Request Example**:
```http
GET /admin/users?page=1&limit=20&role=USER&isActive=true
Authorization: Bearer {admin_token}
```

**Response** (200 OK):
```json
{
  "data": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "email": "user1@example.com",
      "firstName": "John",
      "lastName": "Doe",
      "organization": "Water NGO",
      "role": "USER",
      "isActive": true,
      "storageUsedMb": 12.3,
      "createdAt": "2024-01-20T10:30:00Z",
      "lastLogin": "2024-01-21T09:15:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 45,
    "totalPages": 3
  }
}
```

**Errors**:
- `401`: Unauthorized
- `403`: Forbidden (USER role attempting admin endpoint)

---

#### POST /admin/users/{userId}/deactivate

Deactivate a user account (admin only).

**Access**: Authenticated (ADMIN role required)

**Request**:
```json
{
  "reason": "Policy violation - uploaded inappropriate content"
}
```

**Response** (200 OK):
```json
{
  "message": "User account deactivated successfully",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "deactivatedAt": "2024-01-21T11:00:00Z"
}
```

**Side Effects**:
- User's JWT tokens are invalidated immediately
- User cannot log in until reactivated
- Audit log entry created

**Errors**:
- `404`: User not found
- `403`: Forbidden (cannot deactivate another admin or self)

---

#### PUT /admin/users/{userId}/role

Change user's role (admin only).

**Access**: Authenticated (ADMIN role required)

**Request**:
```json
{
  "role": "ADMIN"
}
```

**Response** (200 OK):
```json
{
  "message": "User role updated successfully",
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "oldRole": "USER",
  "newRole": "ADMIN",
  "updatedAt": "2024-01-21T11:30:00Z"
}
```

**Side Effects**:
- User's existing JWT tokens are invalidated (must re-login to get new token with new role)
- Audit log entry created

**Errors**:
- `400`: Invalid role value (must be USER or ADMIN)
- `404`: User not found
- `403`: Forbidden (cannot change own role)

---

## Service 3: Data Service

**Technology**: Spring Boot 3.2, Apache Commons CSV, PostGIS
**Port**: 8082 (internal only)
**Responsibilities**: Data upload, validation, storage, provenance tracking

### Endpoints

#### POST /data/upload/hydro

Upload hydrological data CSV file.

**Access**: Authenticated (USER role)

**Request Headers**:
```http
Authorization: Bearer {token}
Content-Type: multipart/form-data
```

**Request Body** (multipart):
```
file: hydro_data.csv (binary)
```

**CSV Format Requirements**:

Required columns:
- `source` (string): Data source (e.g., "USGS", "Field Survey")
- `latitude` (float): -90 to 90
- `longitude` (float): -180 to 180
- `measurement_value` (float): Measured value
- `measurement_unit` (string): Unit (e.g., "mg/L", "µg/L", "pH")
- `measurement_date` (ISO 8601): Date of measurement

Optional columns:
- `data_type` (string): Parameter name (e.g., "arsenic", "fluoride", "pH")
- `location_name` (string): Site name
- `depth_meters` (float): Sampling depth
- `notes` (text): Additional notes

**Example CSV**:
```csv
source,data_type,location_name,latitude,longitude,measurement_value,measurement_unit,measurement_date
Field Survey,arsenic,Well A,34.0522,-118.2437,75.5,µg/L,2024-01-15T10:30:00
USGS,fluoride,River Site 1,34.0600,-118.2500,2.3,mg/L,2024-01-14T14:00:00
Field Survey,pH,Borehole 3,34.0550,-118.2400,6.8,pH,2024-01-16T08:00:00
```

**Response** (200 OK):
```json
{
  "uploadId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "status": "SUCCESS",
  "filename": "hydro_data.csv",
  "fileSize": 2457600,
  "fileSizeDisplay": "2.34 MB",
  "checksum": "sha256:a3b5c7d9e1f2...",
  "recordsImported": 127,
  "recordsFailed": 3,
  "validationErrors": [
    {
      "row": 5,
      "column": "latitude",
      "value": "91.5",
      "error": "Latitude must be between -90 and 90"
    },
    {
      "row": 23,
      "column": "measurement_date",
      "value": "2024-13-01",
      "error": "Invalid date format. Expected ISO 8601 (YYYY-MM-DD or YYYY-MM-DDTHH:MM:SS)"
    }
  ],
  "storageUsed": {
    "currentMb": 14.6,
    "quotaMb": 100,
    "percentUsed": 14.6
  },
  "uploadedAt": "2024-01-21T12:00:00Z"
}
```

**Errors**:
- `400`: Missing required columns, invalid file format
- `413`: File size exceeds 10MB limit
- `422`: Validation errors (all rows failed)
- `507`: Storage quota exceeded

**Error Response Example (Quota Exceeded)**:
```json
{
  "error": "STORAGE_QUOTA_EXCEEDED",
  "message": "Upload would exceed your storage quota. Current usage: 95.2 MB, Quota: 100 MB, Requested: 8.1 MB",
  "currentUsageMb": 95.2,
  "quotaMb": 100,
  "requestedMb": 8.1,
  "availableMb": 4.8,
  "timestamp": "2024-01-21T12:00:00Z",
  "requestId": "7a8b9c0d-1234-5678-9abc-def012345678"
}
```

**Processing Notes**:
- File is processed synchronously (may take 5-10 seconds for 10MB file)
- Data provenance tracked: original filename, checksum (SHA-256), upload timestamp, user ID
- Rows with validation errors are skipped; valid rows are imported
- If all rows fail validation, upload is rejected (422 error)

---

#### POST /data/upload/community

Upload community data CSV file.

**Access**: Authenticated (USER role)

**CSV Format Requirements**:

Required columns:
- `community_name` (string): Name of community
- `latitude` (float): -90 to 90
- `longitude` (float): -180 to 180
- `population` (integer): Population count (must be > 0)

Optional columns:
- `water_access_level` (enum): none | limited | basic | safely_managed (WHO JMP classification)
- `source` (string): Data source

**Example CSV**:
```csv
community_name,latitude,longitude,population,water_access_level,source
Village A,34.0522,-118.2437,5000,limited,Field Survey
Village B,34.0600,-118.2500,3000,basic,OpenStreetMap
Town C,34.0550,-118.2400,12000,safely_managed,Government Census
```

**Response**: Same structure as hydro upload

---

#### POST /data/upload/infrastructure

Upload infrastructure data CSV file.

**Access**: Authenticated (USER role)

**CSV Format Requirements**:

Required columns:
- `facility_type` (enum): borehole | well | treatment_plant | reservoir | pipeline | water_point
- `facility_name` (string): Name of facility
- `latitude` (float): -90 to 90
- `longitude` (float): -180 to 180
- `operational_status` (enum): operational | non_operational | under_maintenance

Optional columns:
- `capacity` (float): Capacity value
- `capacity_unit` (string): Unit (e.g., "liters_per_day", "cubic_meters")
- `construction_year` (integer): Year built
- `last_maintenance_date` (date): Last serviced

**Example CSV**:
```csv
facility_type,facility_name,latitude,longitude,operational_status,capacity,capacity_unit
borehole,Well 1,34.0522,-118.2437,operational,3000,liters_per_day
treatment_plant,Plant A,34.0600,-118.2500,operational,10000,liters_per_day
reservoir,Reservoir B,34.0550,-118.2400,operational,50000,cubic_meters
```

**Response**: Same structure as hydro upload

---

#### GET /data/datasets

List user's uploaded datasets.

**Access**: Authenticated (USER role)

**Query Parameters**:
- `page`: Page number (default: 1)
- `limit`: Items per page (default: 20, max: 100)
- `dataType`: Filter by type (hydro | community | infrastructure)
- `sortBy`: Sort field (uploadedAt | filename | fileSize)
- `sortOrder`: asc | desc (default: desc)

**Request Example**:
```http
GET /data/datasets?page=1&limit=20&dataType=hydro&sortBy=uploadedAt&sortOrder=desc
Authorization: Bearer {token}
```

**Response** (200 OK):
```json
{
  "data": [
    {
      "id": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
      "filename": "hydro_data.csv",
      "dataType": "hydro",
      "fileSizeMb": 2.34,
      "checksum": "sha256:a3b5c7d9e1f2...",
      "recordsImported": 127,
      "recordsFailed": 3,
      "uploadedAt": "2024-01-21T12:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 5,
    "totalPages": 1
  },
  "storageUsed": {
    "currentMb": 14.6,
    "quotaMb": 100,
    "percentUsed": 14.6
  }
}
```

---

#### GET /data/datasets/{datasetId}

Get dataset metadata and preview.

**Access**: Authenticated (USER role, own data only)

**Response** (200 OK):
```json
{
  "id": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "filename": "hydro_data.csv",
  "dataType": "hydro",
  "fileSizeMb": 2.34,
  "checksum": "sha256:a3b5c7d9e1f2...",
  "recordsImported": 127,
  "recordsFailed": 3,
  "validationErrors": [],
  "uploadedAt": "2024-01-21T12:00:00Z",
  "preview": [
    {
      "source": "Field Survey",
      "data_type": "arsenic",
      "location_name": "Well A",
      "latitude": 34.0522,
      "longitude": -118.2437,
      "measurement_value": 75.5,
      "measurement_unit": "µg/L",
      "measurement_date": "2024-01-15T10:30:00Z"
    }
  ],
  "previewNote": "Showing first 10 rows"
}
```

**Errors**:
- `404`: Dataset not found
- `403`: Forbidden (dataset belongs to another user)

---

#### DELETE /data/datasets/{datasetId}

Delete a dataset (soft delete).

**Access**: Authenticated (USER role, own data only)

**Response** (200 OK):
```json
{
  "message": "Dataset deleted successfully",
  "datasetId": "a1b2c3d4-5678-90ab-cdef-1234567890ab",
  "deletedAt": "2024-01-21T13:00:00Z",
  "storageFreedMb": 2.34
}
```

**Side Effects**:
- Dataset marked as deleted (soft delete, `deleted_at` timestamp set)
- Storage quota updated
- Associated data in hydro_data/community_data/infrastructure_data tables remains (cascade handled by database)

**Errors**:
- `404`: Dataset not found
- `403`: Forbidden (dataset belongs to another user)
- `409`: Cannot delete dataset used in existing risk assessments (must delete assessments first)

---

#### GET /data/storage

Get user's storage quota information.

**Access**: Authenticated (USER role)

**Response** (200 OK):
```json
{
  "quotaMb": 100,
  "usedMb": 14.6,
  "availableMb": 85.4,
  "percentUsed": 14.6,
  "datasets": {
    "hydro": 3,
    "community": 2,
    "infrastructure": 1
  },
  "breakdown": [
    {
      "dataType": "hydro",
      "datasets": 3,
      "totalMb": 8.2
    },
    {
      "dataType": "community",
      "datasets": 2,
      "totalMb": 4.1
    },
    {
      "dataType": "infrastructure",
      "datasets": 1,
      "totalMb": 2.3
    }
  ]
}
```

---

#### GET /admin/stats

Get system-wide statistics (admin only).

**Access**: Authenticated (ADMIN role required)

**Response** (200 OK):
```json
{
  "users": {
    "total": 45,
    "active": 42,
    "admins": 2
  },
  "datasets": {
    "total": 203,
    "hydro": 85,
    "community": 72,
    "infrastructure": 46
  },
  "storage": {
    "totalUsedMb": 1234.5,
    "totalQuotaMb": 4500,
    "percentUsed": 27.4
  },
  "uploads": {
    "last24Hours": 18,
    "last7Days": 87
  },
  "assessments": {
    "total": 156,
    "last24Hours": 12,
    "last7Days": 64
  },
  "timestamp": "2024-01-21T14:00:00Z"
}
```

---

## Service 4: Worker Service

**Technology**: FastAPI 0.104, Python 3.11, asyncpg
**Port**: 8000 (internal only)
**Responsibilities**: Risk score calculation, analysis, export generation

### Endpoints

#### POST /analysis/risk-score

Calculate risk scores for communities.

**Access**: Authenticated (USER role)

**Request**:
```json
{
  "name": "January 2024 Assessment",
  "hydroDatasetIds": ["uuid-1", "uuid-2"],
  "communityDatasetIds": ["uuid-3"],
  "infrastructureDatasetIds": ["uuid-4"]
}
```

**Validations**:
- At least one hydro dataset and one community dataset required
- Infrastructure dataset optional
- User must own all specified datasets
- Max 10 datasets per request

**Response** (200 OK):
```json
{
  "assessmentId": "b2c3d4e5-6789-01bc-def1-234567890bcd",
  "name": "January 2024 Assessment",
  "status": "COMPLETED",
  "algorithmVersion": "1.0.0",
  "summary": {
    "totalCommunities": 23,
    "highRisk": 7,
    "mediumRisk": 11,
    "lowRisk": 5,
    "totalPopulation": 145000,
    "highRiskPopulation": 42000
  },
  "results": [
    {
      "communityId": 123,
      "communityName": "Village A",
      "coordinates": {
        "latitude": 34.0522,
        "longitude": -118.2437
      },
      "population": 5000,
      "riskScore": 78,
      "riskLevel": "HIGH",
      "confidenceLevel": "HIGH",
      "explanation": {
        "summary": "High risk due to elevated arsenic levels and limited access to treatment facilities",
        "factors": [
          {
            "factor": "water_quality",
            "score": 85,
            "impact": "HIGH",
            "details": "Arsenic level (75.5 µg/L) exceeds WHO guideline (10 µg/L) by 65.5 µg/L",
            "recommendation": "Install arsenic removal treatment system immediately"
          },
          {
            "factor": "infrastructure_access",
            "score": 72,
            "impact": "HIGH",
            "details": "Nearest treatment plant is 12.3 km away, exceeding recommended 5 km",
            "recommendation": "Construct local water point with treatment capability"
          },
          {
            "factor": "population_pressure",
            "score": 65,
            "impact": "MEDIUM",
            "details": "Population (5000) exceeds nearest facility capacity (3000 L/day) by 40%",
            "recommendation": "Expand facility capacity or add secondary source"
          }
        ]
      },
      "datasetsUsed": {
        "hydro": ["uuid-1"],
        "community": ["uuid-3"],
        "infrastructure": ["uuid-4"]
      }
    }
  ],
  "calculatedAt": "2024-01-21T15:00:00Z",
  "executionTimeMs": 3456
}
```

**Risk Scoring Algorithm (v1.0.0)**:

Risk score (0-100) calculated from 4 factors:
1. **Water Quality** (0-40 points): Based on WHO guidelines for arsenic, fluoride, nitrate, pH, TDS
2. **Infrastructure Access** (0-25 points): Distance to nearest operational facility
3. **Population Pressure** (0-20 points): Population vs. facility capacity
4. **Environmental Factors** (0-15 points): Seasonal variation, climate data (V1)

Risk levels:
- **HIGH**: 67-100 points (urgent intervention needed)
- **MEDIUM**: 34-66 points (monitoring and planning required)
- **LOW**: 0-33 points (acceptable, continue monitoring)

Confidence levels:
- **HIGH**: Sample size >20 measurements, recent data (<6 months)
- **MEDIUM**: Sample size 5-20 measurements, data <12 months old
- **LOW**: Sample size <5 measurements or data >12 months old
- **NONE**: No data available for factor

**Errors**:
- `400`: Invalid dataset IDs, missing required datasets
- `403`: Forbidden (user doesn't own specified datasets)
- `404`: One or more datasets not found
- `422`: No communities found in community dataset

**Processing Notes**:
- Calculation may take 10-30 seconds for large datasets (100+ communities)
- Results are cached (subsequent fetches are instant)

---

#### GET /analysis/{assessmentId}

Get risk assessment results.

**Access**: Authenticated (USER role, own assessments only)

**Response** (200 OK): Same structure as POST response above

**Errors**:
- `404`: Assessment not found
- `403`: Forbidden (assessment belongs to another user)

---

#### GET /analysis/{assessmentId}/export/csv

Export risk assessment results as CSV.

**Access**: Authenticated (USER role, own assessments only)

**Response** (200 OK):
```csv
community_name,latitude,longitude,population,risk_score,risk_level,confidence_level,water_quality_score,infrastructure_access_score,population_pressure_score,top_recommendation
Village A,34.0522,-118.2437,5000,78,HIGH,HIGH,85,72,65,"Install arsenic removal treatment system immediately"
Village B,34.0600,-118.2500,3000,42,MEDIUM,MEDIUM,55,38,45,"Monitor water quality monthly, plan for capacity expansion"
...
```

**Headers**:
```http
Content-Type: text/csv
Content-Disposition: attachment; filename="risk_assessment_b2c3d4e5_2024-01-21.csv"
```

**Errors**:
- `404`: Assessment not found
- `403`: Forbidden (assessment belongs to another user)

---

#### GET /analysis

List user's risk assessments.

**Access**: Authenticated (USER role)

**Query Parameters**:
- `page`: Page number (default: 1)
- `limit`: Items per page (default: 20, max: 100)
- `sortBy`: uploadedAt | name (default: uploadedAt)
- `sortOrder`: asc | desc (default: desc)

**Response** (200 OK):
```json
{
  "data": [
    {
      "id": "b2c3d4e5-6789-01bc-def1-234567890bcd",
      "name": "January 2024 Assessment",
      "algorithmVersion": "1.0.0",
      "summary": {
        "totalCommunities": 23,
        "highRisk": 7,
        "mediumRisk": 11,
        "lowRisk": 5
      },
      "calculatedAt": "2024-01-21T15:00:00Z"
    }
  ],
  "pagination": {
    "page": 1,
    "limit": 20,
    "total": 8,
    "totalPages": 1
  }
}
```

---

## Inter-Service Communication

### Internal-Only Endpoints

Services communicate internally via HTTP (not exposed to clients). These use a shared secret for authentication.

#### Data Service → Worker Service

**POST http://worker-service:8000/internal/analyze**

Used when Data Service needs to trigger analysis.

**Request Headers**:
```http
X-Internal-Service-Token: {shared_secret}
Content-Type: application/json
```

**Request**:
```json
{
  "userId": "550e8400-e29b-41d4-a716-446655440000",
  "assessmentName": "January 2024 Assessment",
  "hydroData": [
    {
      "id": 1,
      "source": "Field Survey",
      "latitude": 34.0522,
      "longitude": -118.2437,
      "measurement_value": 75.5,
      "measurement_unit": "µg/L",
      "measurement_date": "2024-01-15T10:30:00Z",
      "parameter": "arsenic"
    }
  ],
  "communityData": [
    {
      "id": 123,
      "community_name": "Village A",
      "latitude": 34.0522,
      "longitude": -118.2437,
      "population": 5000,
      "water_access_level": "limited"
    }
  ],
  "infrastructureData": [
    {
      "id": 45,
      "facility_type": "treatment_plant",
      "facility_name": "Plant A",
      "latitude": 34.0600,
      "longitude": -118.2500,
      "operational_status": "operational",
      "capacity": 10000,
      "capacity_unit": "liters_per_day"
    }
  ]
}
```

**Response** (200 OK): Same as public POST /analysis/risk-score

---

## Error Handling

### Standard Error Response Format

All errors follow this structure:

```json
{
  "error": "ERROR_CODE",
  "message": "Human-readable error message",
  "details": {
    "field": "fieldName",
    "value": "providedValue",
    "additionalInfo": "..."
  },
  "timestamp": "2024-01-21T15:00:00Z",
  "requestId": "7a8b9c0d-1234-5678-9abc-def012345678",
  "path": "/api/data/upload/hydro"
}
```

### Common Error Codes

| Code | HTTP Status | Meaning |
|------|-------------|---------|
| `VALIDATION_ERROR` | 400 | Input validation failed |
| `AUTHENTICATION_FAILED` | 401 | Invalid or expired token |
| `ACCOUNT_LOCKED` | 403 | Account locked due to failed attempts |
| `INSUFFICIENT_PERMISSIONS` | 403 | Role check failed |
| `RESOURCE_NOT_FOUND` | 404 | Resource doesn't exist |
| `DUPLICATE_RESOURCE` | 409 | Resource already exists (e.g., email) |
| `STORAGE_QUOTA_EXCEEDED` | 507 | Storage limit reached |
| `RATE_LIMIT_EXCEEDED` | 429 | Too many requests |
| `INTERNAL_SERVER_ERROR` | 500 | Unexpected server error |
| `SERVICE_UNAVAILABLE` | 503 | Downstream service down |

---

## Rate Limiting

Rate limits enforced by API Gateway per user:

| Endpoint Pattern | Limit | Window |
|------------------|-------|--------|
| `/api/auth/*` | 10 requests | per minute |
| `/api/data/*` | 100 requests | per minute |
| `/api/analysis/risk-score` | 20 requests | per minute |
| `/api/analysis/*` (other) | 100 requests | per minute |
| `/api/admin/*` | 50 requests | per minute |

**Rate Limit Headers** (included in all responses):
```http
X-RateLimit-Limit: 100
X-RateLimit-Remaining: 87
X-RateLimit-Reset: 1706000460
```

**Rate Limit Exceeded Response** (429):
```json
{
  "error": "RATE_LIMIT_EXCEEDED",
  "message": "Rate limit exceeded. Try again in 42 seconds.",
  "limit": 100,
  "remaining": 0,
  "resetAt": "2024-01-21T15:01:00Z",
  "retryAfter": 42,
  "timestamp": "2024-01-21T15:00:18Z",
  "requestId": "7a8b9c0d-1234-5678-9abc-def012345678"
}
```

---

## Data Schemas

### Common Data Types

#### Coordinates (GeoJSON Point)

```json
{
  "type": "Point",
  "coordinates": [-118.2437, 34.0522]
}
```

**Note**: GeoJSON uses [longitude, latitude] order (x, y)

#### Timestamp (ISO 8601)

```
2024-01-21T15:00:00Z
```

Always in UTC timezone (Z suffix)

#### UUID

```
550e8400-e29b-41d4-a716-446655440000
```

Version 4 UUID (random)

---

## Appendix: JWT Token Payload

```json
{
  "sub": "550e8400-e29b-41d4-a716-446655440000",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1706000000,
  "exp": 1706086400,
  "jti": "token-unique-id-12345"
}
```

**Claims**:
- `sub`: Subject (user ID)
- `email`: User email
- `role`: USER | ADMIN
- `iat`: Issued at (Unix timestamp)
- `exp`: Expiration (Unix timestamp, 24 hours from `iat`)
- `jti`: JWT ID (for token revocation, V1)

**Signing**: HMAC-SHA256 with `JWT_SECRET` from environment

---

**Document Version**: 1.0
**Status**: Complete
**Next Review**: February 2024
