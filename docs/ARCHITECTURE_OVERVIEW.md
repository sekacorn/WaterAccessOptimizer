# Architecture Overview

WaterAccessOptimizer is currently released as a React frontend plus Java/Spring backend services backed by PostgreSQL, with an optional AI model service for extended analysis workflows.

## Current Release Surface

The release-verified user experience is centered on:

- authentication or frontend demo mode
- CSV upload for hydrological, community, and infrastructure datasets
- map-based review of records
- risk assessment creation and results review
- export of assessment outputs

## Active Runtime Paths

For this release, the practical runtime shape is:

```text
frontend -> auth-service
frontend -> data-service
auth-service -> postgres
data-service -> postgres
optional: frontend/data-service -> ai-model
```

Important packaging note:

- `docker-compose.prod.yml` builds the packaged data-service container from `backend/api-gateway/`
- the repository also contains `backend/data-service/`, but that directory is not the primary packaged runtime path for this release

## Services

### Frontend

- Path: `frontend/`
- Stack: React, Vite, React Router, Zustand, Leaflet, Chart.js
- Responsibilities: authentication UI, uploads, mapping, assessment workflows, exports, demo mode

### Auth Service

- Path: `backend/auth-service/`
- Stack: Java 17, Spring Boot, Spring Security
- Default port: `8081`
- Responsibilities: registration, login, refresh tokens, password reset flow, rate limiting, JWT issuance

### Data Service

- Packaged runtime path: `backend/api-gateway/`
- Default port: `8087`
- Responsibilities: dataset upload, validation, storage, risk assessment orchestration, export endpoints

### Optional AI Model

- Path: `ai-model/`
- Default port: `8000`
- Responsibilities: optional model-driven analysis and prediction support

### Data Store

- PostgreSQL is the primary persistent store for release deployments.

## Frontend Runtime Notes

- Demo mode is frontend-only and seeds a logged-in session with mock data.
- Production frontend delivery uses Nginx with SPA routing and security headers.
- Accessibility improvements in this release include skip navigation, live status messaging, reduced-motion support, and stronger table semantics.

## Security Notes

This release includes several security-readiness improvements:

- no fallback secrets in core Spring configuration
- environment-driven CORS and startup validation
- JWT validation across auth and gateway paths
- authentication rate limiting
- stronger frontend edge headers

The repository should still be treated as deployment-specific for final hardening, secret rotation, TLS, retention, and incident handling.

## Legacy Modules

The repository still contains older or exploratory modules that are not part of the primary routed release surface. They remain useful as references, but they should not be presented as verified production paths without additional validation.

## Release Verification Status

The frontend release path has been verified with:

- `npm run lint`
- `npm run test -- --run`
- `npm run build`

Backend runtime validation remains environment-specific and should be completed by the deployment owner before production release.
