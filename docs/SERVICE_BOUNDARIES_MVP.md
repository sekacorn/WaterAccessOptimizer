# Service Boundaries - MVP Architecture

**Water Access Optimizer - Service Consolidation Plan**

Version: 0.1.0 (MVP)
Date: 2026-02-02

---

## Overview

This document defines the **4 MVP services** based on Agent 01 (Product Requirements) and Agent 15 (System Architecture), and provides a plan to consolidate the current 7+ services in the codebase.

---

## Target MVP Architecture (4 Services)

### Service 1: API Gateway
**Technology**: Spring Cloud Gateway (Spring Boot 3.2)
**Port**: 8080
**Location**: `backend/api-gateway/`
**Status**: [X]Exists, properly configured

**Responsibilities**:
- Route all incoming HTTP requests to backend services
- Validate JWT tokens (extract user ID, email, role)
- Propagate user context via headers (X-User-Id, X-User-Email, X-User-Role, X-Request-Id)
- Rate limiting (100 req/min per user, 20 req/min for analysis endpoints)
- CORS configuration
- Circuit breaker patterns for service resilience

**Routes** (from SERVICE_CONTRACTS.md):
```yaml
Public (no auth):
  /api/auth/register       → auth-service:8086
  /api/auth/login          → auth-service:8086

Protected (USER role):
  /api/auth/**             → auth-service:8086
  /api/data/**             → data-service:8082
  /api/analysis/**         → worker-service:8000

Protected (ADMIN role):
  /api/admin/**            → auth-service:8086
```

**No Data Ownership**: Stateless, stores no data

---

### Service 2: Auth Service
**Technology**: Spring Boot 3.2 + Spring Security 6 + JWT
**Port**: 8086
**Location**: `backend/auth-service/`
**Status**: [X]Exists, properly configured

**Responsibilities**:
- User registration (email/password, validate uniqueness)
- User login (credential validation, JWT token generation)
- JWT token management (generation, validation, expiration)
- Password management (bcrypt hashing, change password)
- Role-Based Access Control (USER, ADMIN roles only in MVP)
- User profile management (view/update own profile)
- Admin user management (list users, deactivate/activate, adjust quotas)
- Audit logging (login attempts, role changes, account modifications)

**API Endpoints**:
```
Public:
  POST   /auth/register
  POST   /auth/login

Protected (USER):
  GET    /auth/me
  PUT    /auth/me
  POST   /auth/change-password

Protected (ADMIN):
  GET    /admin/users
  POST   /admin/users/{id}/deactivate
  POST   /admin/users/{id}/activate
  PUT    /admin/users/{id}/role
  GET    /admin/stats
```

**Data Ownership** (PostgreSQL schema: `auth_schema`):
- `users` table: id, email, password_hash, first_name, last_name, organization, role, is_active, storage_quota_mb, created_at, last_login
- `audit_logs` table: user actions, IP addresses, timestamps

**Dependencies**:
- PostgreSQL database (auth_schema)
- Redis (optional) for session blacklist/token invalidation

---

### Service 3: Data Service
**Technology**: Spring Boot 3.2 + PostGIS
**Port**: 8082
**Location**: `backend/data-service/` (⚠️ **needs creation** OR rename `water-integrator`)
**Status**: ⚠️ Service doesn't exist; `water-integrator` exists with similar responsibilities

**Responsibilities**:
- CSV file upload (hydro, community, infrastructure data types)
- File validation (size ≤10MB, MIME type, required columns)
- CSV parsing (Apache Commons CSV library)
- Data validation (coordinates: lat -90 to 90, lon -180 to 180; dates: ISO 8601)
- PostgreSQL storage with PostGIS spatial columns
- Data provenance tracking (SHA-256 checksum, original filename, upload timestamp, user ID)
- Dataset management (list, view, delete with soft delete)
- Storage quota enforcement (100MB per user default, prevent uploads over quota)
- Data retrieval APIs for Worker Service consumption

**API Endpoints**:
```
POST   /data/upload/hydro
POST   /data/upload/community
POST   /data/upload/infrastructure
GET    /data/datasets              (paginated, sortable)
GET    /data/datasets/{id}
DELETE /data/datasets/{id}          (soft delete)
GET    /data/datasets/{id}/preview  (first 10 rows)
GET    /data/storage               (quota info)

Internal APIs (for Worker Service):
GET    /data/hydro?datasetIds=...
GET    /data/community?datasetIds=...
GET    /data/infrastructure?datasetIds=...
```

**Data Ownership** (PostgreSQL schema: `data_schema`):
- `uploads` table: id, user_id, original_filename, file_checksum (SHA-256), file_size_bytes, data_type, row_count, uploaded_at, deleted_at
- `hydro_data` table: id, upload_id, coordinates (PostGIS GEOGRAPHY), measurement_value, measurement_unit, measurement_date, data_type, source, location_name
- `community_data` table: id, upload_id, community_name, coordinates, population, water_access_level, source
- `infrastructure_data` table: id, upload_id, facility_type, facility_name, coordinates, operational_status, capacity, capacity_unit
- `data_validation_errors` table: id, upload_id, row_number, column_name, error_message

**File Storage**:
- CSV files processed in-memory, data extracted to PostgreSQL
- Original files NOT stored permanently (data in database only)
- Temporary storage during processing only

**Dependencies**:
- PostgreSQL with PostGIS extension (data_schema)
- Apache Commons CSV, Jackson JSON library

---

### Service 4: Worker Service
**Technology**: FastAPI (Python 3.10+) + asyncpg + pandas + numpy
**Port**: 8000
**Location**: `ai-model/` (⚠️ **rename to** `worker-service/`)
**Status**: ⚠️ Exists as `ai-model` but needs refactoring

**Responsibilities**:
- Risk score calculation using rule-based algorithm (0-100 score)
  - 4 factors: water quality (35%), access distance (30%), infrastructure reliability (25%), population pressure (10%)
- Explainability generation (top 3 contributing factors with measured values, guidelines, plain-language impact)
- Data confidence level calculation (HIGH: >30 samples, MEDIUM: 10-30, LOW: 1-9, NONE: 0)
- Spatial queries via PostGIS (nearest facility, distance calculations)
- Data provenance tracking for analyses (datasets used, algorithm version, timestamp)
- CSV export of risk assessment results
- Aggregations and statistics for dashboard
- **Not in MVP**: ML predictions, PyTorch models (V1 feature)

**API Endpoints**:
```
POST   /analysis/risk-score
GET    /analysis/{id}
GET    /analysis/{id}/provenance
GET    /analysis/{id}/export/csv
GET    /analysis
GET    /analysis/algorithm/v1.0.0  (algorithm description in markdown)
```

**Data Ownership** (PostgreSQL schema: `analysis_schema`):
- `risk_assessments` table: id, user_id, name, algorithm_version, created_at
- `assessment_datasets` table: assessment_id, dataset_id, dataset_type (provenance link)
- `risk_results` table: id, assessment_id, community_id, community_name, coordinates, population, risk_score (0-100), risk_level (HIGH/MEDIUM/LOW), confidence_level (HIGH/MEDIUM/LOW/NONE), explanation_json (JSONB with top 3 factors), calculated_at

**Dependencies**:
- PostgreSQL with PostGIS (analysis_schema + read access to data_schema)
- FastAPI, asyncpg, pandas, numpy
- **NOT needed in MVP**: PyTorch, scikit-learn (V1 features)

**Refactoring Needed**:
- Remove MBTI personalization logic (lines 246-326 in water_predictor.py)
- Remove PyTorch model loading (untrained model, not needed for MVP rule-based scoring)
- Implement deterministic rule-based risk algorithm per Agent 04 (OPTIMIZATION_ENGINE)
- Add proper explainability with WHO guideline references

---

## Current Codebase vs Target Architecture

### Services to KEEP ([X])

| Current Service | Target Service | Port | Action |
|----------------|----------------|------|--------|
| `api-gateway` | API Gateway | 8080 | [X]Keep as-is |
| `auth-service` | Auth Service | 8086 | [X]Keep as-is |

### Services to RENAME (🔄)

| Current Service | Target Service | Port | Action |
|----------------|----------------|------|--------|
| `water-integrator` | Data Service | 8082 | 🔄 Rename to `data-service` OR merge logic into new `data-service` |
| `ai-model` | Worker Service | 8000 | 🔄 Rename folder to `worker-service`, refactor code |

### Services to REMOVE (❌ - V1/V2 Features)

| Current Service | Port | Reason | Target Version |
|----------------|------|--------|----------------|
| `llm-service` | 8084 | Natural language queries not in MVP scope | V1 (Months 3-6) |
| `collaboration-service` | 8085 | Real-time WebSocket collaboration not in MVP | V1 (Months 3-6) |
| `water-visualizer` | 8082 | Visualization logic moves to frontend (Leaflet.js) + Worker Service backend | **Deprecate** (split responsibilities) |
| `user-session` | 8083 | Duplicate of auth-service, JWT-based auth already in auth-service | **Remove** (redundant) |

---

## Service Consolidation Plan

### Phase 1: Remove Redundant Services (Week 1)

**1.1 Remove `user-session` service**
```bash
# Backup first
mv backend/user-session backend/_archived/user-session

# Update docker-compose.yml - remove user-session service
# Update any references in gateway routes
```

**Rationale**: Auth Service already handles JWT-based authentication. `user-session` is redundant.

---

**1.2 Remove `llm-service` and `collaboration-service`**
```bash
# These are V1 features, not MVP
mv backend/llm-service backend/_archived/llm-service-V1
mv backend/collaboration-service backend/_archived/collaboration-service-V1
```

**Rationale**: MVP focuses on core data upload and risk assessment. LLM and collaboration are V1 enhancements.

---

### Phase 2: Rename/Refactor Services (Week 2)

**2.1 Rename `water-integrator` to `data-service`**

**Option A: Simple Rename**
```bash
mv backend/water-integrator backend/data-service

# Update:
# - pom.xml: <artifactId>data-service</artifactId>
# - application.yml: spring.application.name=data-service
# - Main class: DataServiceApplication.java
# - docker-compose.yml: service name
```

**Option B: Create New (if water-integrator has conflicting code)**
```bash
# Copy structure
cp -r backend/water-integrator backend/data-service
# Clean up and refactor to match Agent 01/15 specs
```

**Verify API Endpoints**:
- Ensure all endpoints use `/data/` prefix (not `/integrator/`)
- Verify routes in api-gateway match SERVICE_CONTRACTS.md

---

**2.2 Refactor `ai-model` to `worker-service`**
```bash
mv ai-model worker-service

# Update:
# - Folder structure: worker-service/app/main.py (FastAPI app)
# - requirements.txt: Remove PyTorch, torchvision (V1 dependencies)
# - Remove MBTI logic from water_predictor.py
# - Implement rule-based risk algorithm per Agent 04
```

**Key Refactoring Tasks**:
1. **Remove**: PyTorch model loading (untrained, not needed)
2. **Remove**: MBTI personalization (lines 246-326 in water_predictor.py)
3. **Add**: Rule-based risk scoring algorithm with WHO guidelines
4. **Add**: Explainability with top 3 factors (measured value, guideline, impact statement)
5. **Add**: Confidence level calculation (based on sample size, data recency)
6. **Add**: Data provenance tracking (datasets used, checksums, algorithm version)

---

**2.3 Handle `water-visualizer` responsibilities**

`water-visualizer` has two types of logic:

**Backend Logic (3D viz, AI predictions)** → Move to Worker Service (or remove if not MVP):
- 3D visualization endpoints → ❌ Remove (not in MVP, use 2D Leaflet.js)
- AI prediction endpoints → ❌ Remove (not in MVP, rule-based only)

**Frontend Logic (React UI)** → Already in `frontend/` folder:
- Map visualization → [X]Keep in frontend (Leaflet.js)
- Dataset list UI → [X]Keep in frontend
- Risk results UI → [X]Keep in frontend

**Action**:
```bash
# Archive water-visualizer (logic already split between frontend and worker-service)
mv backend/water-visualizer backend/_archived/water-visualizer

# Verify frontend has all visualization components:
# - frontend/src/components/MapView.jsx (Leaflet.js map)
# - frontend/src/pages/Analyze.jsx (data upload)
# - frontend/src/pages/Explore.jsx (map exploration)
```

---

### Phase 3: Update Configuration Files (Week 2)

**3.1 Update `docker-compose.yml`**

**Before** (7 services):
```yaml
services:
  api-gateway:
  auth-service:
  water-integrator:
  water-visualizer:
  user-session:
  llm-service:
  collaboration-service:
  ai-model:
  frontend:
  postgres:
  redis:
```

**After** (4 services):
```yaml
services:
  api-gateway:      # [X]Keep
  auth-service:     # [X]Keep
  data-service:     # 🔄 Renamed from water-integrator
  worker-service:   # 🔄 Renamed from ai-model
  frontend:         # [X]Keep
  postgres:         # [X]Keep (PostGIS extension)
  redis:            # [X]Keep (caching)
  prometheus:       # [X]Keep (monitoring)
  grafana:          # [X]Keep (monitoring)
```

---

**3.2 Update `api-gateway` routes**

Verify `application.yml` in api-gateway matches SERVICE_CONTRACTS.md:
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: auth-register
          uri: lb://auth-service  # port 8086
          predicates:
            - Path=/api/auth/register

        - id: data-upload
          uri: lb://data-service  # port 8082 (was water-integrator)
          predicates:
            - Path=/api/data/**
          filters:
            - JwtAuthenticationFilter

        - id: worker-analysis
          uri: lb://worker-service  # port 8000 (was ai-model)
          predicates:
            - Path=/api/analysis/**
          filters:
            - JwtAuthenticationFilter
```

---

**3.3 Update Frontend API calls**

Verify `frontend/src/services/api.js` uses correct endpoints:
```javascript
// Correct (MVP):
export const uploadHydroData = (file) => api.post('/api/data/upload/hydro', formData)
export const getDatasets = () => api.get('/api/data/datasets')
export const runRiskAssessment = (payload) => api.post('/api/analysis/risk-score', payload)

// Incorrect (to fix):
api.post('/api/integrator/upload/hydro')  // ❌ Old path
api.post('/api/ai/predict')               // ❌ V1 feature, not MVP
api.post('/api/llm/query')                // ❌ V1 feature
```

---

### Phase 4: Update Documentation (Week 2)

**4.1 Files to Update**:
- [X]`README.md` - Already updated in Iteration 1
- [X]`docs/ARCHITECTURE_OVERVIEW.md` - Already correct
- [X]`docs/SERVICE_CONTRACTS.md` - Already correct
- 🔄 `GETTING_STARTED.md` - Update service list
- 🔄 `docker-compose.yml` - Remove extra services
- 🔄 `docs/DEPLOYMENT_RUNBOOK.md` - Update service list

**4.2 Create Migration Guide** (this document)

---

## Verification Checklist

After consolidation, verify:

### Service Count
- [ ] Exactly 4 backend services running: api-gateway, auth-service, data-service, worker-service
- [ ] Frontend running as static SPA (React)
- [ ] Infrastructure services: postgres, redis, prometheus, grafana

### API Endpoints
- [ ] All endpoints use `/api/data/` (not `/api/integrator/`)
- [ ] No `/api/llm/` endpoints (V1 feature)
- [ ] No `/api/collaboration/` endpoints (V1 feature)
- [ ] No `/api/visualizer/` endpoints (moved to frontend)

### Database Schemas
- [ ] `auth_schema`: users, audit_logs
- [ ] `data_schema`: uploads, hydro_data, community_data, infrastructure_data
- [ ] `analysis_schema`: risk_assessments, assessment_datasets, risk_results

### Ports
- [ ] API Gateway: 8080
- [ ] Auth Service: 8086
- [ ] Data Service: 8082
- [ ] Worker Service: 8000
- [ ] Frontend: 3000 (dev) / served by NGINX (prod)
- [ ] PostgreSQL: 5432
- [ ] Redis: 6379

### Docker Compose
```bash
docker-compose ps
# Should show exactly 4 services + frontend + postgres + redis + monitoring
```

### Health Checks
```bash
curl http://localhost:8080/actuator/health  # API Gateway
curl http://localhost:8086/actuator/health  # Auth Service
curl http://localhost:8082/actuator/health  # Data Service
curl http://localhost:8000/health           # Worker Service
```

---

## Inter-Service Communication

### Data Flow: Risk Assessment
```
1. User uploads CSV → Frontend → API Gateway → Data Service → PostgreSQL (data_schema)
2. User clicks "Run Analysis" → Frontend → API Gateway → Worker Service
3. Worker Service fetches data → HTTP GET → Data Service → PostgreSQL query
4. Worker Service calculates risk scores → PostgreSQL (analysis_schema)
5. Worker Service returns results → API Gateway → Frontend
```

### Authentication Flow
```
1. User registers → Frontend → API Gateway → Auth Service → PostgreSQL (auth_schema)
2. User logs in → Frontend → API Gateway → Auth Service (validates, generates JWT)
3. User makes request → Frontend (includes JWT) → API Gateway (validates JWT, adds headers) → Backend Service
4. Backend Service trusts headers (X-User-Id, X-User-Role) from API Gateway
```

---

## Summary

### MVP Services (4)
1. **API Gateway** (Spring Cloud Gateway, port 8080)
2. **Auth Service** (Spring Boot + JWT, port 8086)
3. **Data Service** (Spring Boot + PostGIS, port 8082) - *rename from water-integrator*
4. **Worker Service** (FastAPI + Python, port 8000) - *refactor from ai-model*

### Removed Services (V1/V2)
- `llm-service` → V1 (natural language queries)
- `collaboration-service` → V1 (real-time WebSocket collaboration)
- `water-visualizer` → Deprecated (split: frontend UI + worker service backend)
- `user-session` → Redundant (auth-service handles JWT auth)

### Key Changes
- Use `/api/data/` endpoints (not `/api/integrator/`)
- Remove MBTI personalization completely
- Remove PyTorch/ML predictions (V1 feature)
- Use rule-based risk scoring in MVP
- Frontend handles all visualization with Leaflet.js

---

## Next Steps (Iteration 3)

1. Apply Agent 16 (Security/IAM Architecture)
2. Implement JWT propagation in API Gateway
3. Refactor auth-service security configuration
4. Add rate limiting and audit logging
5. Update gateway routes with role-based authorization filters

---

**Document Status**: [X]Complete
**Last Updated**: 2026-02-02
**Iteration**: 2
