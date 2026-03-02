# Architecture Overview

**Water Access Optimizer - MVP Architecture**

Version: 0.1.0 (MVP)
Last Updated: January 26, 2024

---

## Table of Contents

1. [System Overview](#system-overview)
2. [Architecture Principles](#architecture-principles)
3. [Service Architecture](#service-architecture)
4. [Data Flow](#data-flow)
5. [Technology Stack](#technology-stack)
6. [Service Contracts](#service-contracts)
7. [Database Schema](#database-schema)
8. [Security Architecture](#security-architecture)
9. [Deployment Architecture](#deployment-architecture)
10. [Scalability Considerations](#scalability-considerations)

---

## System Overview

Water Access Optimizer is a **decision support tool** for water infrastructure planning and risk assessment. The MVP follows a **microservices architecture** with 4 core services, designed for modularity, testability, and eventual scalability.

### Design Goals (MVP)

- [X]**Simplicity**: Minimal service count (4 services vs. 7+ in original plan)
- [X]**Testability**: Each service independently testable
- [X]**Deployability**: Single `docker-compose up` for local development
- [X]**Observability**: Prometheus metrics, structured logging, health checks
- [X]**Security**: JWT-based auth, RBAC, rate limiting

### Not in MVP Scope

- ❌ Real-time collaboration (WebSocket complexity)
- ❌ LLM natural language queries (API costs, complexity)
- ❌ 3D visualization (large bundle size)
- ❌ Enterprise features (SSO, MFA, org management)
- ❌ External API connectors (USGS, WHO auto-import)

---

## Architecture Principles

### 1. **Service Boundaries by Business Capability**

Each service owns a distinct business capability:
- **Auth Service**: User management and authentication
- **Data Service**: Data ingestion, validation, and storage
- **Worker Service**: Analytics, risk scoring, and computations
- **API Gateway**: Routing, authentication propagation, rate limiting

### 2. **Database Per Service** (Shared PostgreSQL, Separate Schemas)

While services share a PostgreSQL instance (cost optimization for MVP), each service has its own schema:
- `auth_schema`: User accounts, roles, tokens
- `data_schema`: Uploaded datasets, provenance tracking
- `analysis_schema`: Risk assessments, results

This allows future migration to separate databases without application changes.

### 3. **Synchronous Communication** (HTTP/REST)

All inter-service communication uses synchronous REST APIs for MVP:
- Simpler debugging and testing
- No message broker complexity
- Suitable for MVP scale (<100 concurrent users)

**Future (V1)**: Introduce async messaging (RabbitMQ/Kafka) for long-running tasks.

### 4. **Stateless Services** (Session in Redis)

All services are stateless; session state stored in Redis:
- Enables horizontal scaling
- Simplifies deployment
- Allows rolling updates without downtime

### 5. **Observability First**

Every service exposes:
- `/actuator/health` (liveness and readiness probes)
- `/actuator/prometheus` (metrics endpoint)
- Structured JSON logs with correlation IDs

---

## Service Architecture

### High-Level Diagram

```
┌─────────────────────────────────────────────────────┐
│                                                     │
│                 Users (Browser)                     │
│                                                     │
└───────────────────────┬─────────────────────────────┘
                        │
                        │ HTTPS (TLS 1.2+)
                        │
┌───────────────────────▼─────────────────────────────┐
│                                                     │
│            NGINX (Reverse Proxy)                    │
│         - TLS termination                           │
│         - Static file serving (/assets)             │
│         - Load balancing (future)                   │
│                                                     │
└───────────────┬───────────────────┬─────────────────┘
                │                   │
         ┌──────▼──────┐     ┌──────▼──────────┐
         │             │     │                 │
         │  Frontend   │     │   API Gateway   │
         │  (React)    │     │   (Spring)      │
         │             │     │                 │
         │  Port: 3000 │     │   Port: 8080    │
         │             │     │                 │
         └─────────────┘     └────────┬────────┘
                                      │
                        ┌─────────────┼─────────────┐
                        │             │             │
                 ┌──────▼──────┐ ┌───▼──────┐ ┌────▼──────┐
                 │             │ │          │ │           │
                 │Auth Service │ │Data Svc  │ │Worker Svc │
                 │(Spring Boot)│ │(Spring)  │ │(FastAPI)  │
                 │             │ │          │ │           │
                 │Port: 8086   │ │Port: 8082│ │Port: 8000 │
                 │             │ │          │ │           │
                 └──────┬──────┘ └────┬─────┘ └─────┬─────┘
                        │             │             │
                        └─────────────┼─────────────┘
                                      │
                        ┌─────────────┼──────────────┐
                        │             │              │
                 ┌──────▼──────┐  ┌──▼───────┐  ┌───▼──────┐
                 │             │  │          │  │          │
                 │ PostgreSQL  │  │  Redis   │  │Prometheus│
                 │  + PostGIS  │  │  Cache   │  │ Metrics  │
                 │             │  │          │  │          │
                 │ Port: 5432  │  │Port: 6379│  │Port: 9090│
                 │             │  │          │  │          │
                 └─────────────┘  └──────────┘  └──────────┘
```

---

## Data Flow

### 1. User Registration Flow

```
1. User → Frontend: Fill registration form
2. Frontend → API Gateway: POST /api/auth/register
3. API Gateway → Auth Service: Forward request
4. Auth Service:
   - Validate email format, password strength
   - Hash password with bcrypt
   - Insert user into auth_schema.users
   - Generate JWT token
5. Auth Service → API Gateway → Frontend: Return token
6. Frontend: Store token in localStorage, redirect to dashboard
```

### 2. Data Upload Flow

```
1. User → Frontend: Select CSV file, click Upload
2. Frontend → API Gateway: POST /api/data/upload/hydro (multipart/form-data)
3. API Gateway:
   - Validate JWT token
   - Check rate limit (100 req/min per user)
   - Forward to Data Service
4. Data Service:
   - Validate file size (<10MB)
   - Check user storage quota (<100MB)
   - Parse CSV (validate columns, data types)
   - Store metadata in data_schema.uploads
   - Store records in data_schema.hydro_data
   - Calculate SHA-256 checksum
5. Data Service → API Gateway → Frontend: Return upload_id, validation results
6. Frontend: Display success message or validation errors
```

### 3. Risk Assessment Flow

```
1. User → Frontend: Select datasets, click "Calculate Risk"
2. Frontend → API Gateway: POST /api/analysis/risk-score
3. API Gateway:
   - Validate JWT token
   - Forward to Data Service (verify dataset ownership)
4. Data Service → Worker Service: POST /internal/analyze/risk
   - Send hydro_data, community_data, infrastructure_data as JSON
5. Worker Service (Python):
   - Calculate risk scores (rule-based algorithm)
   - Apply WHO thresholds for water quality
   - Calculate distance to nearest infrastructure
   - Compute population pressure factor
   - Aggregate into composite risk score (0-100)
   - Generate explainability factors
6. Worker Service → Data Service: Return risk results
7. Data Service:
   - Store results in analysis_schema.risk_assessments
   - Associate with datasets (provenance)
8. Data Service → API Gateway → Frontend: Return risk scores
9. Frontend: Display color-coded map and results table
```

---

## Technology Stack

### Frontend

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Framework | React 18 | Component-based UI |
| Build Tool | Vite 4 | Fast dev server, optimized builds |
| Styling | Tailwind CSS 3 | Utility-first styling |
| Maps | Leaflet.js + React Leaflet | 2D interactive maps |
| Charts | Recharts | Data visualization |
| State Management | React Context + hooks | Lightweight state management |
| HTTP Client | Axios | API requests |
| Routing | React Router 6 | Client-side routing |

### Backend Services

#### API Gateway (Spring Cloud Gateway)
- **Language**: Java 17
- **Framework**: Spring Boot 3.2, Spring Cloud Gateway 4.0
- **Purpose**: Request routing, JWT validation, rate limiting, CORS
- **Port**: 8080

#### Auth Service (Spring Boot)
- **Language**: Java 17
- **Framework**: Spring Boot 3.2, Spring Security 6
- **Purpose**: User management, JWT token generation, password hashing
- **Port**: 8086
- **Dependencies**: Spring Data JPA, BCrypt, JWT (io.jsonwebtoken)

#### Data Service (Spring Boot)
- **Language**: Java 17
- **Framework**: Spring Boot 3.2, Spring Data JPA
- **Purpose**: CSV/GeoJSON upload, validation, storage, provenance tracking
- **Port**: 8082
- **Dependencies**: Apache Commons CSV, PostGIS JDBC, Hibernate Spatial

#### Worker Service (FastAPI)
- **Language**: Python 3.10
- **Framework**: FastAPI 0.104, Pydantic
- **Purpose**: Risk score calculation, data analysis, CSV export generation
- **Port**: 8000
- **Dependencies**: NumPy, Pandas, GeoPandas

### Data Layer

#### PostgreSQL 15 + PostGIS 3.3
- **Purpose**: Primary data store with spatial capabilities
- **Storage**: User data, uploaded datasets, risk assessments
- **Size**: 5GB target for MVP

#### Redis 7
- **Purpose**: Session storage, API response caching
- **TTL**: 24 hours for sessions, 1 hour for cached API responses

### Infrastructure

| Component | Technology | Purpose |
|-----------|-----------|---------|
| Containerization | Docker 20.10+ | Service isolation |
| Orchestration (Local) | Docker Compose 1.29+ | Multi-container management |
| Orchestration (Prod) | Kubernetes 1.27+ | Production deployment |
| Reverse Proxy | NGINX 1.24 | TLS termination, static files |
| Monitoring | Prometheus 2.45, Grafana 10 | Metrics and dashboards |
| CI/CD | GitHub Actions | Automated testing, deployment |

---

## Service Contracts

### API Gateway Routes

```yaml
# Authentication
POST   /api/auth/register       → Auth Service (8086)
POST   /api/auth/login          → Auth Service (8086)
POST   /api/auth/refresh-token  → Auth Service (8086)
GET    /api/auth/me             → Auth Service (8086)

# Data Management
POST   /api/data/upload/hydro       → Data Service (8082)
POST   /api/data/upload/community   → Data Service (8082)
POST   /api/data/upload/infrastructure → Data Service (8082)
GET    /api/data/datasets           → Data Service (8082)
DELETE /api/data/datasets/{id}      → Data Service (8082)

# Analysis
POST   /api/analysis/risk-score     → Data Service → Worker Service
GET    /api/analysis/{id}           → Data Service (8082)
GET    /api/analysis/{id}/export/csv → Worker Service (8000)

# Admin (ADMIN role only)
GET    /api/admin/users             → Auth Service (8086)
POST   /api/admin/users/{id}/deactivate → Auth Service (8086)
GET    /api/admin/stats             → Data Service (8082)

# Health & Metrics
GET    /actuator/health             → API Gateway (8080)
GET    /actuator/prometheus         → API Gateway (8080)
```

### Inter-Service Communication

#### Data Service → Worker Service

```http
POST http://worker-service:8000/internal/analyze/risk
Authorization: X-Internal-Service-Token: {shared-secret}
Content-Type: application/json

{
  "hydro_data": [
    { "lat": 34.05, "lon": -118.25, "arsenic_ppm": 75.5 }
  ],
  "community_data": [
    { "id": 123, "name": "Village A", "lat": 34.05, "lon": -118.25, "population": 5000 }
  ],
  "infrastructure_data": [
    { "type": "borehole", "lat": 34.06, "lon": -118.26, "capacity_lpd": 3000 }
  ]
}

Response: 200 OK
{
  "results": [
    {
      "community_id": 123,
      "risk_score": 78,
      "risk_level": "HIGH",
      "factors": [...]
    }
  ]
}
```

---

## Database Schema

### Auth Schema (auth_schema)

```sql
-- Users table
CREATE TABLE auth_schema.users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email VARCHAR(255) UNIQUE NOT NULL,
    password_hash VARCHAR(60) NOT NULL, -- bcrypt
    first_name VARCHAR(100),
    last_name VARCHAR(100),
    organization VARCHAR(255),
    role VARCHAR(20) DEFAULT 'USER', -- USER, ADMIN
    is_active BOOLEAN DEFAULT true,
    failed_login_attempts INT DEFAULT 0,
    locked_until TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON auth_schema.users(email);
CREATE INDEX idx_users_role ON auth_schema.users(role);

-- Audit log
CREATE TABLE auth_schema.audit_log (
    id SERIAL PRIMARY KEY,
    user_id UUID REFERENCES auth_schema.users(id),
    action VARCHAR(50) NOT NULL, -- LOGIN, LOGOUT, ROLE_CHANGE
    ip_address INET,
    user_agent TEXT,
    success BOOLEAN,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### Data Schema (data_schema)

```sql
-- Uploads metadata
CREATE TABLE data_schema.uploads (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    filename VARCHAR(255) NOT NULL,
    data_type VARCHAR(50) NOT NULL, -- hydro, community, infrastructure
    file_size_bytes BIGINT NOT NULL,
    checksum_sha256 CHAR(64) NOT NULL,
    records_imported INT DEFAULT 0,
    records_failed INT DEFAULT 0,
    validation_errors JSONB,
    uploaded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_uploads_user ON data_schema.uploads(user_id);
CREATE INDEX idx_uploads_type ON data_schema.uploads(data_type);

-- Hydrological data
CREATE TABLE data_schema.hydro_data (
    id SERIAL PRIMARY KEY,
    upload_id UUID REFERENCES data_schema.uploads(id) ON DELETE CASCADE,
    source VARCHAR(100) NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL, -- PostGIS
    measurement_value NUMERIC(10,2),
    measurement_unit VARCHAR(50),
    measurement_date TIMESTAMP NOT NULL,
    parameter_name VARCHAR(100), -- arsenic, pH, TDS, etc.
    depth_meters NUMERIC(10,2)
);

CREATE INDEX idx_hydro_location ON data_schema.hydro_data USING GIST(location);
CREATE INDEX idx_hydro_upload ON data_schema.hydro_data(upload_id);

-- Community data
CREATE TABLE data_schema.community_data (
    id SERIAL PRIMARY KEY,
    upload_id UUID REFERENCES data_schema.uploads(id) ON DELETE CASCADE,
    community_name VARCHAR(255) NOT NULL,
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    population INT NOT NULL,
    water_access_level VARCHAR(50) -- none, limited, basic, safely_managed
);

CREATE INDEX idx_community_location ON data_schema.community_data USING GIST(location);

-- Infrastructure data
CREATE TABLE data_schema.infrastructure_data (
    id SERIAL PRIMARY KEY,
    upload_id UUID REFERENCES data_schema.uploads(id) ON DELETE CASCADE,
    facility_type VARCHAR(100) NOT NULL, -- borehole, treatment_plant, reservoir
    facility_name VARCHAR(255),
    location GEOGRAPHY(POINT, 4326) NOT NULL,
    operational_status VARCHAR(50), -- operational, non_operational, under_maintenance
    capacity NUMERIC(10,2),
    capacity_unit VARCHAR(50) -- liters_per_day, cubic_meters
);

CREATE INDEX idx_infrastructure_location ON data_schema.infrastructure_data USING GIST(location);
```

### Analysis Schema (analysis_schema)

```sql
-- Risk assessments
CREATE TABLE analysis_schema.risk_assessments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    hydro_dataset_ids UUID[] NOT NULL,
    community_dataset_ids UUID[] NOT NULL,
    infrastructure_dataset_ids UUID[],
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_assessments_user ON analysis_schema.risk_assessments(user_id);

-- Risk results (one per community)
CREATE TABLE analysis_schema.risk_results (
    id SERIAL PRIMARY KEY,
    assessment_id UUID REFERENCES analysis_schema.risk_assessments(id) ON DELETE CASCADE,
    community_id INT REFERENCES data_schema.community_data(id),
    community_name VARCHAR(255),
    location GEOGRAPHY(POINT, 4326),
    risk_score NUMERIC(5,2) NOT NULL, -- 0-100
    risk_level VARCHAR(10) NOT NULL, -- LOW, MEDIUM, HIGH
    factors JSONB NOT NULL -- {water_quality: {...}, infrastructure: {...}}
);

CREATE INDEX idx_results_assessment ON analysis_schema.risk_results(assessment_id);
CREATE INDEX idx_results_risk_level ON analysis_schema.risk_results(risk_level);
```

---

## Security Architecture

### Authentication Flow

```
1. User submits email + password
2. Auth Service validates credentials:
   - Check if account locked (5 failed attempts → 30-min lockout)
   - Verify password with bcrypt
3. If valid:
   - Reset failed_login_attempts to 0
   - Generate JWT token (24-hour expiration)
   - Log successful login to audit_log
4. If invalid:
   - Increment failed_login_attempts
   - Lock account if attempts >= 5
   - Log failed login to audit_log
5. Return JWT token to client
```

### JWT Token Structure

```json
{
  "sub": "user-uuid",
  "email": "user@example.com",
  "role": "USER",
  "iat": 1705832400,
  "exp": 1705918800
}
```

Signed with HMAC-SHA256 using `JWT_SECRET` from environment.

### Authorization Enforcement

**API Gateway** validates JWT on every request:
1. Extract `Authorization: Bearer {token}` header
2. Verify signature and expiration
3. Extract `user_id` and `role` from claims
4. Add `X-User-ID` and `X-User-Role` headers for downstream services
5. If invalid/expired → 401 Unauthorized

**Downstream services** trust `X-User-ID` and `X-User-Role` headers (internal network only).

### Rate Limiting

API Gateway enforces rate limits using Redis:
- Key: `rate_limit:{user_id}`
- Limit: 100 requests per minute
- Algorithm: Token bucket
- Response: `429 Too Many Requests` with `Retry-After` header

### Input Validation

All services validate inputs:
- **Auth Service**: Email format, password strength (min 8 chars, 1 uppercase, 1 lowercase, 1 number)
- **Data Service**: File size (<10MB), column presence, data types, coordinate bounds
- **Worker Service**: Numeric ranges, required fields

### SQL Injection Prevention

All database queries use **parameterized statements** (JPA for Java, SQLAlchemy for Python). No string concatenation for SQL.

---

## Deployment Architecture

### Local Development (Docker Compose)

```yaml
version: '3.8'

services:
  postgres:
    image: postgis/postgis:15-3.3
    volumes:
      - postgres_data:/var/lib/postgresql/data
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}

  redis:
    image: redis:7-alpine

  api-gateway:
    build: ./backend/api-gateway
    ports:
      - "8080:8080"
    depends_on:
      - auth-service
      - data-service

  auth-service:
    build: ./backend/auth-service
    environment:
      DB_HOST: postgres
      REDIS_HOST: redis
      JWT_SECRET: ${JWT_SECRET}

  data-service:
    build: ./backend/data-service
    environment:
      DB_HOST: postgres

  worker-service:
    build: ./ai-model
    environment:
      DB_HOST: postgres

  frontend:
    build: ./frontend
    ports:
      - "3000:3000"

  prometheus:
    image: prom/prometheus:v2.45.0
    volumes:
      - ./infra/prometheus/prometheus.yml:/etc/prometheus/prometheus.yml

  grafana:
    image: grafana/grafana:10.0.0
    ports:
      - "3001:3000"

volumes:
  postgres_data:
```

### Production (Kubernetes)

```yaml
# Simplified architecture
Namespace: water-optimizer

Deployments:
  - api-gateway (replicas: 2)
  - auth-service (replicas: 2)
  - data-service (replicas: 2)
  - worker-service (replicas: 2)
  - frontend (replicas: 2)

StatefulSets:
  - postgres (replicas: 1) # Future: replicas: 3 with replication
  - redis (replicas: 1)

Services:
  - api-gateway (LoadBalancer)
  - postgres (ClusterIP)
  - redis (ClusterIP)

Ingress:
  - TLS termination with cert-manager
  - Routes: / → frontend, /api → api-gateway

ConfigMaps:
  - Application configs (log levels, API URLs)

Secrets:
  - Database credentials
  - JWT secret
  - TLS certificates

HorizontalPodAutoscaler:
  - api-gateway: min=2, max=10, targetCPU=70%
  - data-service: min=2, max=8, targetCPU=70%
```

---

## Scalability Considerations

### MVP Limits (v0.1.0)

- **Concurrent Users**: 100
- **Total Storage**: 20GB (18GB usable, 2GB buffer)
- **Database Size**: 5GB target
- **Per-User Quota**: 100MB
- **Request Rate**: 100 req/min per user

### Bottlenecks & Solutions

| Bottleneck | V1 Solution |
|------------|-------------|
| CSV parsing (blocking I/O) | Async task queue (Celery + RabbitMQ) |
| Risk calculation (CPU-bound) | Horizontal scaling of Worker Service |
| Database writes (single instance) | PostgreSQL replication (primary + 2 replicas) |
| File storage (disk-based) | S3-compatible object storage (MinIO) |
| Map rendering (1000+ markers) | Marker clustering (Leaflet.markercluster) |

### Scaling Strategy (V1)

```
100 users → 500 users:
- Scale api-gateway: 2 → 4 replicas
- Scale data-service: 2 → 4 replicas
- Add read replicas for PostgreSQL

500 users → 1000 users:
- Introduce caching layer (Redis cluster)
- Async processing for uploads (RabbitMQ)
- CDN for static assets (CloudFlare)

1000+ users:
- Multi-region deployment
- Database sharding by user_id
- Message queue for inter-service communication
```

---

## Future Architecture Changes (Post-MVP)

### V1 Additions (Months 3-6)

1. **LLM Service** (FastAPI + Python)
   - Natural language queries
   - Context-aware prompts
   - Rate limiting for API costs

2. **Collaboration Service** (Spring Boot + WebSocket)
   - Real-time shared workspaces
   - Conflict resolution (CRDT)
   - Presence tracking

3. **Async Processing** (Celery + RabbitMQ)
   - Background CSV parsing
   - Scheduled reports
   - Email notifications

4. **Object Storage** (MinIO/S3)
   - Move uploaded files from disk to object storage
   - Presigned URLs for direct uploads

### V2 Additions (Month 6+)

1. **API Management** (Kong/Apigee)
   - API versioning
   - Third-party API keys
   - Usage analytics

2. **Search Service** (Elasticsearch)
   - Full-text search across datasets
   - Faceted search
   - Geospatial queries

3. **Notification Service** (Spring Boot + Email/SMS)
   - Email alerts for high-risk assessments
   - SMS notifications (Twilio)
   - Webhook integrations

---

## Monitoring & Observability

### Metrics (Prometheus)

**Business Metrics**:
- `uploads_total` (counter) - Total file uploads
- `risk_assessments_total` (counter) - Total risk assessments run
- `active_users` (gauge) - Active users in last 24 hours

**Technical Metrics**:
- `http_requests_total` (counter) - HTTP request count by endpoint, method, status
- `http_request_duration_seconds` (histogram) - Request latency (p50, p95, p99)
- `database_query_duration_seconds` (histogram) - Database query latency
- `database_connections_active` (gauge) - Active database connections

**System Metrics**:
- `disk_usage_bytes` (gauge) - Disk space used
- `memory_usage_bytes` (gauge) - JVM/process memory
- `cpu_usage_percent` (gauge) - CPU utilization

### Alerts (Prometheus Alertmanager)

```yaml
- alert: HighErrorRate
  expr: rate(http_requests_total{status=~"5.."}[5m]) > 0.05
  for: 5m
  labels:
    severity: critical

- alert: SlowAPIResponse
  expr: histogram_quantile(0.95, http_request_duration_seconds) > 0.5
  for: 10m
  labels:
    severity: warning

- alert: DiskSpaceCritical
  expr: disk_usage_bytes > 17179869184  # 16GB
  for: 5m
  labels:
    severity: critical
```

### Logging

All services emit structured JSON logs:

```json
{
  "timestamp": "2024-01-20T10:30:00Z",
  "level": "INFO",
  "service": "data-service",
  "correlation_id": "abc-123-def",
  "user_id": "user-uuid",
  "message": "CSV file uploaded successfully",
  "metadata": {
    "filename": "hydro_data.csv",
    "records_imported": 127,
    "file_size_mb": 2.3
  }
}
```

Correlation IDs propagate across all services via `X-Correlation-ID` header.

---

## References

- [Service Contracts](SERVICE_CONTRACTS.md) - Detailed API specifications
- [Database Schema](../database/postgres/schema.sql) - Complete SQL DDL
- [Deployment Runbook](DEPLOYMENT_RUNBOOK.md) - Production deployment guide
- [Security Guide](../agent_pack/16_SECURITY_IAM.md) - Security specifications
- [Testing Strategy](../agent_pack/10_TESTING_QA.md) - Test coverage requirements

---

**Document Version**: 1.0
**Status**: Complete
**Next Review**: February 2024
