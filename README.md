# WaterAccessOptimizer

## 1. Project Name and Short Mission Statement

WaterAccessOptimizer is a free, inspectable, self-hostable water access planning application.

The mission is to reduce software costs for nonprofits, universities, researchers, students, community organizations, civic technologists, and public-interest teams that need practical tools for water access data work. The project gives these teams code they can run, inspect, fork, modify, and adapt for research or mission-driven use instead of depending only on expensive closed platforms.

## 2. The Problem

Water access work often sits at the intersection of field research, public health, infrastructure planning, community data, mapping, and reporting. Teams doing this work may need software for data upload, validation, maps, risk scoring, dashboards, exports, and deployment, but commercial tools can be too expensive or too restrictive.

This creates practical problems:

- Cost: nonprofits, students, research groups, and grant-funded teams may not be able to afford recurring GIS, analytics, dashboard, or custom software fees.
- Access: teams may need software they can run locally, teach with, modify, or deploy on low-cost infrastructure.
- Research transparency: water access findings should be traceable to source data, scoring logic, assumptions, and documentation.
- Workflow fragmentation: hydrological data, community survey data, infrastructure records, maps, and reports often end up split across spreadsheets and separate tools.
- Privacy and data control: uploaded datasets may include account data, community records, operational infrastructure information, or sensitive public-interest data that operators need to control.
- Compliance readiness: public-sector, education, infrastructure, and research deployments may need privacy, accessibility, security, audit, and governance review before live use.
- Accessibility: public-interest tools should be usable by people who rely on keyboard navigation, screen readers, reduced-motion settings, and clear non-visual summaries.
- AI risk: automated risk scoring or future AI/LLM features must not be mistaken for professional water safety, engineering, legal, or public health decisions.
- Infrastructure constraints: teams may need a Docker Compose path first, while treating Kubernetes or regulated cloud deployment as later validation work.

## 3. What This Solves

WaterAccessOptimizer provides a working starting point for water access data analysis:

- A React frontend for login, dashboard review, CSV upload, map review, risk assessment creation, results, privacy, accessibility, and security/compliance pages.
- CSV workflows for hydrological, community, and infrastructure datasets.
- Data validation concepts and backend code for required fields, coordinates, dates, file limits, storage quota, and validation feedback.
- Map workflows using Leaflet on the frontend and GeoJSON-style backend responses.
- Risk assessment workflows that classify communities or records into risk levels and show summaries, charts, tables, and exports.
- Deterministic risk scoring code in `ai-model/risk_scoring.py` that uses weighted factors for water quality, access distance, infrastructure, and population pressure.
- Template-based risk summaries that include confidence and safety disclaimers rather than relying on an LLM.
- A frontend demo mode with seeded mock data for screenshots, teaching, walkthroughs, and early stakeholder review.
- Docker Compose deployment files for a local full-stack release path with PostgreSQL, backend services, frontend, optional AI model service, Prometheus, and Grafana.
- Documentation for deployment, architecture, data ingestion, validation, compliance readiness, operations, service boundaries, and PostGIS queries.

This project does not replace field experts, licensed engineers, public health officials, legal counsel, accessibility auditors, security assessors, or local regulators. It gives teams a lower-cost software base for organizing and reviewing water access data before making decisions through appropriate professional processes.

## 4. Who It Is For

- Nonprofits working on water access, WASH, public health, climate resilience, infrastructure, or community planning.
- Universities that need a teaching, lab, capstone, or research platform students can inspect and modify.
- Researchers who need reproducible workflows for water quality, access, infrastructure, and risk analysis.
- Students building public-interest projects, theses, demos, or field-data prototypes.
- Public-interest teams that need transparent software rather than closed vendor tooling.
- Civic technologists building practical tools for local agencies or community partners.
- Community organizations that need a low-cost way to explore local water access data.
- Local agencies, public-sector teams, and grant-funded programs that need a starting point for self-hosted water access analysis.

## 5. Free / Low-Cost Use

This repository is licensed under the Apache License 2.0. The license grants a no-charge, royalty-free copyright license and patent license to use, reproduce, modify, prepare derivative works, publicly display, publicly perform, sublicense, and distribute the work, subject to the Apache-2.0 terms.

Users can clone, fork, run, modify, and deploy their own copy:

```bash
git clone <this-repository-url>
cd WaterAccessOptimizer
cp .env.example .env
docker-compose -f docker-compose.prod.yml up -d --build
```

If users redistribute modified versions, they must follow the Apache-2.0 requirements, including preserving the license and required notices. The software is provided "AS IS", without warranties or guarantees.

The repository does include contact information in `LICENSE`:

- Author: Sekacorn
- Contact: Sekacorn@gmail.com
- Copyright 2025 Sekacorn

## 6. What Is Included

Current release-facing pieces:

- `frontend/`: React 18, Vite, React Router, Zustand, Leaflet, Chart.js/Recharts, Lucide icons, Vitest, Playwright config, Nginx config, and Dockerfile.
- Frontend pages for landing, login, registration, dashboard, data upload, map view, risk assessment, assessment results, privacy notice, accessibility statement, and security/compliance.
- Frontend demo mode through `npm run dev:demo`, mock API services, and seeded demo data.
- `backend/auth-service/`: Java 17/Spring service for registration, login, refresh tokens, password reset flow, JWT behavior, audit logging, rate limiting, MFA/SSO-related code paths, and health/metrics support.
- `backend/api-gateway/`: Spring Cloud Gateway service with CORS, circuit breaker routes, JWT-related filtering code, logging, actuator endpoints, and the packaged `data-service` runtime path used by `docker-compose.prod.yml`.
- `backend/data-service/`: richer Spring code for CSV upload, validation, map queries, risk assessment APIs, quota handling, PDF/Excel export, Flyway migrations, and tests. Repo docs identify this as present but not the primary packaged runtime path for this release.
- Other backend modules: `water-integrator`, `water-visualizer`, `llm-service`, `collaboration-service`, and `user-session`. These are legacy, exploratory, or future-facing modules unless separately validated.
- `ai-model/`: FastAPI/Python service files, deterministic risk scoring, template-based summary generation, and a PyTorch predictor. The PyTorch predictor attempts to load `model.pt`; if that file is absent, it uses random initialization, so it should not be treated as a trained production model.
- `database/`: PostgreSQL schemas, migration/reference SQL, seed data, Redis config, triggers, and helper functions.
- `docs/`: architecture, compliance readiness, API contracts, data ingestion, data validation, deployment runbook, operations, ML/LLM roadmap, security TODOs, service boundaries, service contracts, PostGIS queries, validation examples, and WHO guideline references.
- `sample_data/`: sample hydrological, community, and infrastructure CSV files for demos and validation experiments.
- `tests/fixtures/`: fixture data for auth, upload, and risk-assessment scenarios.
- `.github/workflows/ci-cd.yml`: CI/CD workflow for frontend checks, backend Maven checks for selected services, E2E tests, Docker image builds, Kubernetes deployment steps, and Trivy scanning.
- `docker-compose.yml`, `docker-compose.dev.yml`, and `docker-compose.prod.yml`: local/full-stack deployment definitions.
- `k8s/`: Kubernetes manifests for namespace, deployments, ingress, config, secrets, Postgres, PVC, and HPA.
- `infra/`: Nginx, Prometheus, Grafana dashboards, datasources, and alert configuration.
- Root screenshots and `frontend/demo-screenshots/` assets.

Current vs planned or not fully verified:

- Docker Compose is the primary documented release path.
- Kubernetes files exist, but the docs treat Kubernetes as separate validation work.
- MFA and SSO guides are retained as planning/historical references; they are not advertised as verified release features.
- LLM and advanced ML features are roadmap/future work. The MVP approach is deterministic scoring and template-based summaries.
- Some docs are historical or planning references and should be checked against the active runtime path before implementation decisions.

## 7. Compliance and Trust Posture

This repository is compliance-aware, not compliance-certified. It includes useful foundations for privacy, security, accessibility, and AI governance discussions, but deployment owners must complete their own legal, security, operational, and accessibility review.

| Standard or Area | Why It May Matter | Support in This Repo | Still Deployment or Organization Responsibility |
| --- | --- | --- | --- |
| GDPR | Accounts, uploads, assessment results, audit trails, telemetry, and community datasets may include personal or sensitive operational data. | Privacy notice page, minimization-oriented account model, authenticated access, audit-friendly backend flows, and docs describing lawful basis, retention, and request handling needs. | Determine lawful basis, retention schedule, processor/subprocessor list, data subject request process, breach workflow, transfer assessment, DPIA need, and deployment-specific privacy notice. |
| European Accessibility Act / EN 301 549 | Public-interest and education tools may need accessible digital interfaces in European contexts. | Accessibility statement, keyboard navigation work, skip link, visible focus styling, live regions, reduced-motion support, text summaries for charts, and accessibility docs. | Complete manual keyboard, screen reader, color contrast, mobile, chart, and map accessibility testing; publish an accessibility contact and remediation process. |
| EU AI Act | Risk scoring, future ML forecasting, and future LLM query features may require transparency, human oversight, and risk classification review. | Deterministic scoring is inspectable; template summaries include disclaimers; ML/LLM roadmap states AI is optional and future LLM outputs must not make safety decisions. | Classify the system under the EU AI Act for the intended use, document human oversight, validate models, manage data quality, monitor outputs, and decide whether any use case is high-risk. |
| NIS2 Directive | Water and public infrastructure contexts may fall into essential or important entity cybersecurity governance depending on operator and jurisdiction. | Environment-based secrets, JWT auth, rate limiting, health checks, Prometheus/Grafana config, security TODOs, CI checks, and deployment docs. | Determine NIS2 applicability, assign security governance, incident reporting, supply-chain controls, risk management, backup, logging, vulnerability handling, and business continuity processes. |
| Cyber Resilience Act | Software distributed for EU use may need secure-by-design practices, vulnerability handling, and product security documentation. | Security features note, fail-fast secret validation, dependency and quality checks, Trivy workflow, Docker/Kubernetes config, and security docs. | Establish vulnerability disclosure, SBOM/release evidence if required, patch process, support period, secure configuration guidance, and CRA conformity obligations if applicable. |
| Section 508 / WCAG 2.1 AA | US public-sector and education deployments may need accessible interfaces. | Accessibility statement, skip link, focus styling, live regions, table semantics, reduced-motion support, and text summaries for visual charts. | Complete formal WCAG/Section 508 testing, remediate map/chart gaps, document exceptions, publish support contact, and run assistive technology review. |
| NIST SP 800-53 | Public-sector or grant-funded deployments may need mapped security and privacy controls. | Authentication, rate limiting, audit-oriented services, environment secrets, CI checks, monitoring config, and compliance readiness docs. | Map controls to the deployment boundary, define control owners, document implementation statements, test controls, retain evidence, and manage authorization artifacts. |
| NIST Cybersecurity Framework | Useful for organizing identify/protect/detect/respond/recover work for infrastructure-adjacent software. | Architecture docs, security TODOs, deployment docs, monitoring config, logging-related code, and CI/security scanning. | Build a CSF profile, define risk tolerance, incident response, recovery, vulnerability management, asset inventory, backup, and third-party risk processes. |
| FedRAMP readiness | Relevant only if a US federal cloud deployment is pursued. There is no authorization evidence in this repo. | Docker/Kubernetes files, CI, security scanning, and docs can support planning conversations. | FedRAMP authorization, system security plan, control implementation, continuous monitoring, boundary definition, cloud provider selection, 3PAO assessment, and agency authorization remain entirely separate work. |

Professional review note: because this project touches water access, public infrastructure, community data, education/research use, and possible public-sector deployment, teams should involve appropriate water quality experts, engineers, security professionals, privacy counsel, accessibility reviewers, and local authorities before using outputs for live decisions.

AI note: deterministic or AI-assisted outputs in this repository are decision-support signals, not final professional determinations. The current PyTorch predictor should not be used as a trained production model unless it is trained, validated, documented, and governed for the intended deployment.

## 8. Current Status

WaterAccessOptimizer is best described as an MVP release and public-interest starter kit. It is useful for demos, research prototypes, teaching, local evaluation, and as a base for mission-driven adaptation. It should not be treated as a turnkey certified production system.

Known limitations:

- Full-stack production readiness is deployment-specific and needs operator validation.
- Docker Compose is the primary documented release path; Kubernetes needs separate testing.
- The packaged release data-service path in `docker-compose.prod.yml` builds from `backend/api-gateway/`, while a separate `backend/data-service/` contains richer but not primary-packaged functionality.
- Demo mode uses mock frontend data and does not validate backend behavior.
- The PyTorch predictor may run with random initialization if no trained `model.pt` exists.
- LLM features, advanced ML forecasting, full SSO/MFA release flows, and collaboration features are not verified release features.
- Compliance docs are readiness/planning documentation, not certifications, audit reports, or legal opinions.
- Security work still needs deployment-specific TLS, secret rotation, penetration/configuration testing, backup policy, incident response, dependency remediation, and operational ownership.
- Accessibility work still needs manual assistive technology testing and stronger non-visual alternatives for maps and charts.

## 9. Quick Start

### Frontend demo mode

This is the simplest working path for reviewing the UI without backend setup.

```bash
cd frontend
npm install
npm run dev:demo
```

Open [http://localhost:5173](http://localhost:5173).

### Docker release stack

Use this for the documented full-stack local release path.

```bash
cp .env.example .env
# Edit .env and replace placeholder secrets.
docker-compose -f docker-compose.prod.yml up -d --build
```

Required values from `.env.example`:

```env
POSTGRES_PASSWORD=change-me
JWT_SECRET=change-me-with-at-least-32-characters
GRAFANA_PASSWORD=change-me
```

Useful URLs:

- Frontend: [http://localhost](http://localhost)
- Auth service: [http://localhost:8081](http://localhost:8081)
- Data service: [http://localhost:8087](http://localhost:8087)
- AI model: [http://localhost:8000](http://localhost:8000)
- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3000](http://localhost:3000)

See [GETTING_STARTED.md](./GETTING_STARTED.md) and [DEPLOYMENT.md](./DEPLOYMENT.md) for the current release guidance.

## 10. Project Structure

```text
.
|-- frontend/                 React/Vite app, tests, mock API, demo screenshots, Nginx config
|-- backend/
|   |-- auth-service/          Spring authentication service
|   |-- api-gateway/           Spring gateway and packaged release data-service path
|   |-- data-service/          Richer upload/map/risk/export service code and migrations
|   `-- other services/        Legacy, exploratory, or future modules
|-- ai-model/                  FastAPI/Python scoring, summaries, and predictor code
|-- database/                  SQL schemas, migrations, seeds, Redis config, triggers
|-- docs/                      Architecture, compliance, validation, API, ops, roadmap docs
|-- infra/                     Prometheus, Grafana, and Nginx infrastructure config
|-- k8s/                       Kubernetes manifests requiring separate validation
|-- sample_data/               Example CSV datasets
|-- tests/fixtures/            Shared test fixtures
|-- .github/workflows/         CI/CD workflow
|-- docker-compose*.yml        Local and release stack definitions
|-- .env.example               Required and optional environment variables
`-- LICENSE                    Apache-2.0 license and contact information
```

## 11. Testing

Frontend commands from `frontend/package.json`:

```bash
cd frontend
npm run lint
npm run test -- --run
npm run build
npm run test:e2e
```

Other available frontend scripts:

```bash
npm run dev
npm run dev:demo
npm run preview
npm run test:ui
npm run test:coverage
npm run test:e2e:ui
npm run test:e2e:report
```

Backend Maven tests can be run from individual service directories that include `pom.xml` and tests, for example:

```bash
cd backend/auth-service
mvn test

cd ../api-gateway
mvn test

cd ../data-service
mvn test
```

The GitHub Actions workflow runs frontend lint/test/build, Maven build/test for `auth-service` and `api-gateway`, Playwright E2E tests, OWASP Dependency Check for selected backend services, Docker image builds, Kubernetes rollout steps, and Trivy scanning. The docs say frontend lint, tests, and build passed during release preparation; backend and full-stack runtime validation remain environment-specific.

No dedicated Python test command for `ai-model/` is documented in the repo.

## 12. License

WaterAccessOptimizer is licensed under the Apache License 2.0. See [LICENSE](./LICENSE).

Apache-2.0 permits no-charge use, reproduction, modification, distribution, sublicensing, and creation of derivative works under the license terms. It also includes patent license language and an "AS IS" warranty disclaimer.
