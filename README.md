# WaterAccessOptimizer

WaterAccessOptimizer is a free, inspectable, self-hostable water access planning tool for mission-driven teams that need to upload field data, map communities and water infrastructure, and run explainable risk assessments without paying for a closed commercial platform.

## The Problem

Nonprofits, universities, researchers, students, public agencies, and public-interest teams often need practical software for water security work, but the tools available to them can be expensive, hard to modify, or locked behind vendor workflows. That creates several concrete problems:

- Cost: small teams may not have budget for recurring software licenses, hosted GIS products, analytics platforms, or custom dashboards.
- Access: students, field researchers, and community organizations may need tools they can run locally for teaching, pilots, grant work, or field validation.
- Research transparency: water access decisions should be traceable to source data, scoring logic, and documented assumptions instead of hidden inside black-box services.
- Workflow friction: teams often manage hydrological measurements, community survey data, infrastructure records, map review, and risk reports in separate tools.
- Privacy and compliance readiness: sensitive community and operational data may need to stay under the operator's control, with deployment-specific decisions about retention, access, incident response, and legal obligations.
- Infrastructure constraints: many teams need a path that works on ordinary local development machines or low-cost servers before moving to larger cloud or Kubernetes deployments.

This repository addresses those problems by providing a runnable full-stack application that can be cloned, inspected, modified, and deployed by the organizations doing the work.

## What This Solves

WaterAccessOptimizer gives teams one practical starting point for water access analysis:

- Upload hydrological, community, and infrastructure CSV datasets instead of stitching records together by hand.
- Validate uploaded data for file size, required columns, coordinates, dates, enum values, storage quota, and other quality issues.
- Review communities, facilities, and water measurements on a map using GeoJSON-style APIs and a Leaflet-based frontend.
- Create water access risk assessments that combine water quality, access distance, infrastructure reliability, and population pressure.
- Show risk levels, confidence levels, charts, tables, summaries, and exports so results can support research, planning, and stakeholder review.
- Run a frontend-only demo mode with seeded mock data for walkthroughs, teaching, screenshots, or proposal discussions.
- Self-host the release stack with Docker Compose using PostgreSQL, Spring backend services, a React frontend, optional AI/risk model service, Prometheus, and Grafana.

The goal is not to replace local expertise or formal engineering review. The goal is to reduce software cost and give mission-driven teams a practical base they can adapt to their own water access data, research methods, and deployment constraints.

## Who It Is For

- Nonprofits working on water access, public health, climate resilience, WASH programs, or infrastructure planning.
- Universities that need a teaching or research platform students can run and inspect.
- Researchers who need reproducible workflows for water quality, service access, and infrastructure risk analysis.
- Students building projects, theses, pilots, or public-interest prototypes.
- Public-interest teams that need transparent, modifiable software instead of a closed vendor workflow.
- Local agencies, civic technology groups, and grant-funded teams that need a low-cost starting point for data-informed water access planning.

## Free / Low-Cost Use

This project is licensed under the Apache License 2.0. The license grants a no-charge, royalty-free copyright license and patent license to use, reproduce, modify, prepare derivative works, publicly display, publicly perform, sublicense, and distribute the work, subject to the Apache-2.0 terms.

In practical terms, eligible users can:

```bash
git clone <this-repository-url>
cd WaterAccessOptimizer
cp .env.example .env
docker-compose -f docker-compose.prod.yml up -d --build
```

They can also fork the repository, adapt the code, run it locally, and deploy their own copy as long as they follow the Apache-2.0 license requirements, including preserving the license and required notices when redistributing modified versions.

The software is provided "AS IS", without warranties or guarantees. Formal compliance, security hardening, privacy obligations, hosting costs, and operational procedures remain the responsibility of the organization deploying it.

The LICENSE file lists:

- Author: Sekacorn
- Contact: Sekacorn@gmail.com
- Copyright 2025 Sekacorn

## What Is Included

Current release surface:

- React/Vite frontend in `frontend/`
- Login, registration, protected routes, dashboard, upload, map, risk assessment, assessment results, privacy, accessibility, and security/compliance pages
- Frontend demo mode using mock API data and an auto-seeded session
- CSV upload workflows for hydrological, community, and infrastructure data
- Upload history, soft delete behavior, and storage quota display
- Map workflows using Leaflet and backend GeoJSON responses
- Risk assessment workflows with HIGH, MEDIUM, and LOW risk levels
- Assessment summaries, chart/table review, and result pages
- Excel and PDF export endpoints for assessment reports
- Java 17/Spring backend services for authentication, gateway/data runtime, and data/risk APIs
- PostgreSQL-backed storage and database schema/migration files
- Optional Python `ai-model/` rule-based risk scoring support with WHO-guideline-oriented factors
- Docker Compose files for development and production-style local deployment
- Nginx frontend container configuration
- Prometheus and Grafana monitoring configuration
- Kubernetes manifests in `k8s/`
- Sample CSV datasets in `sample_data/`
- Unit and E2E test coverage for frontend flows and selected backend services
- Documentation for getting started, deployment, architecture, compliance readiness, API contracts, data ingestion, validation, operations, PostGIS queries, and service boundaries
- Screenshots in the repository root and `frontend/demo-screenshots/`

Current vs planned or reference material:

- The primary documented release path is `docker-compose.prod.yml`.
- `docker-compose.prod.yml` builds the packaged data service runtime from `backend/api-gateway/`.
- A separate `backend/data-service/` exists with richer upload, map, risk, and export code, but the release docs identify it as not the primary packaged runtime path for this release.
- Kubernetes files exist, but the current documentation treats Kubernetes as a separate validation effort.
- Several older or exploratory backend modules remain in the repository, including collaboration, LLM, user-session, water-integrator, and water-visualizer services. They are useful as references or future work, but they should not be presented as the verified production path.
- GeoJSON ingestion, natural-language querying, real-time collaboration, and some ML/LLM ideas are documented as future or exploratory work rather than fully verified MVP behavior.

## Current Status

WaterAccessOptimizer is best described as an MVP release / public-interest starter kit, not a turnkey certified production system.

The frontend release path has been verified in the existing docs with:

```bash
cd frontend
npm run lint
npm run test -- --run
npm run build
```

Backend and full-stack production readiness are deployment-specific. Before using it for live operational decisions, an operator should validate the backend services in their environment, configure real secrets, set TLS and hosting policy, review data retention and privacy obligations, run security testing, and confirm that the risk model and data assumptions fit the local context.

The project includes compliance-readiness work for NIST-aligned secure development, GDPR-oriented privacy-by-design, and Section 508/WCAG accessibility readiness. It does not claim formal certification.

## Screenshots

### Dashboard
![Dashboard](./screenshot-1.png)

### Data Upload
![Data Upload](./screenshot-2.png)

### Map View
![Map View](./screenshot-3.png)

### Risk Assessments
![Risk Assessments](./screenshot-4.png)

### Assessment Results
![Assessment Results](./screenshot-5.png)

### Additional Views
![Additional View 1](./screenshot-6.png)
![Additional View 2](./screenshot-7.png)

## Quick Start

### Frontend demo mode

Use this for UI review, teaching, screenshots, or a no-login walkthrough.

```bash
cd frontend
npm install
npm run dev:demo
```

Open [http://localhost:5173](http://localhost:5173).

### Docker release stack

Use this for the documented full-stack release path.

```bash
cp .env.example .env
# Edit .env and replace placeholder secrets.
docker-compose -f docker-compose.prod.yml up -d --build
```

Primary URLs:

- Frontend: [http://localhost](http://localhost)
- Auth service: [http://localhost:8081](http://localhost:8081)
- Data service: [http://localhost:8087](http://localhost:8087)
- AI model: [http://localhost:8000](http://localhost:8000)
- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3000](http://localhost:3000)

## Documentation

- [GETTING_STARTED.md](./GETTING_STARTED.md)
- [DEPLOYMENT.md](./DEPLOYMENT.md)
- [docs/ARCHITECTURE_OVERVIEW.md](./docs/ARCHITECTURE_OVERVIEW.md)
- [docs/COMPLIANCE_READINESS.md](./docs/COMPLIANCE_READINESS.md)
- [docs/DATA_INGESTION_GUIDE.md](./docs/DATA_INGESTION_GUIDE.md)
- [docs/DATA_VALIDATION_RULES.md](./docs/DATA_VALIDATION_RULES.md)
- [docs/API_CONTRACTS_MVP.md](./docs/API_CONTRACTS_MVP.md)
- [sample_data/README.md](./sample_data/README.md)

## License

Apache License 2.0. See [LICENSE](./LICENSE).
