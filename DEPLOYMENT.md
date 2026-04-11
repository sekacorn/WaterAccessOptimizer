# Deployment Guide

This release is primarily documented and prepared for Docker Compose deployment. Kubernetes manifests remain in the repository, but Compose is the supported release path described here.

## Recommended Release Path

Use `docker-compose.prod.yml`.

```bash
cp .env.example .env
docker-compose -f docker-compose.prod.yml up -d --build
```

## Required Environment Variables

Set these before bringing the stack up:

```env
POSTGRES_PASSWORD=change-me
JWT_SECRET=use-a-long-random-secret
GRAFANA_PASSWORD=change-me
```

Useful optional values:

```env
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost
VITE_API_BASE_URL=http://data-service:8087/api/v1
VITE_AUTH_API_URL=http://auth-service:8081/api/v1/auth
```

## Release Services

`docker-compose.prod.yml` starts:

- `postgres`
- `auth-service`
- `data-service`
- `ai-model`
- `frontend`
- `prometheus`
- `grafana`

Current exposed ports:

- `80` -> frontend
- `3000` -> grafana
- `5432` -> postgres
- `8000` -> ai-model
- `8081` -> auth-service
- `8087` -> data-service
- `9090` -> prometheus

## Important Runtime Note

For this release, the compose file builds the packaged data service from:

```text
backend/api-gateway/
```

That directory is the active packaged runtime path even though the repository also contains:

```text
backend/data-service/
```

Do not rename or swap those paths during release packaging without validating the build and runtime wiring first.

## Release Commands

### Start

```bash
docker-compose -f docker-compose.prod.yml up -d --build
```

### Check status

```bash
docker-compose -f docker-compose.prod.yml ps
docker-compose -f docker-compose.prod.yml logs --tail=100
```

### Follow logs

```bash
docker-compose -f docker-compose.prod.yml logs -f frontend
docker-compose -f docker-compose.prod.yml logs -f auth-service
docker-compose -f docker-compose.prod.yml logs -f data-service
```

### Stop

```bash
docker-compose -f docker-compose.prod.yml down
```

### Stop and remove volumes

```bash
docker-compose -f docker-compose.prod.yml down -v
```

## Health Checks

Use these after startup:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8087/actuator/health
curl http://localhost:8000/health
curl http://localhost/health
```

## Frontend Release Verification

Before cutting the release, run:

```bash
cd frontend
npm install
npm run lint
npm run test -- --run
npm run build
```

These checks passed during the current release-prep pass.

## Suggested Release Checklist

1. Confirm screenshots in `README.md` still match the UI.
2. Confirm `.env` contains real secrets and no placeholders.
3. Run the frontend verification commands.
4. Bring up `docker-compose.prod.yml`.
5. Check auth, data service, and frontend health endpoints.
6. Perform one manual sign-in flow or one demo-mode walkthrough.
7. Verify uploads, map, and assessment pages render correctly.
8. Review [docs/COMPLIANCE_READINESS.md](./docs/COMPLIANCE_READINESS.md) and set operator-owned privacy, retention, incident response, and accessibility support procedures.

## Local Demo Deployment

For stakeholder demos without backend dependencies:

```bash
cd frontend
npm install
npm run dev:demo
```

This mode:

- bypasses login by seeding a demo session
- uses mock API responses
- is intended for screenshots and walkthroughs, not backend validation

## Troubleshooting

### Frontend builds but runtime is broken

Check environment values passed into the frontend container, especially:

- `VITE_API_BASE_URL`
- `VITE_AUTH_API_URL`

### Services fail to connect to Postgres

Check:

- `POSTGRES_PASSWORD`
- `SPRING_DATASOURCE_URL`
- container health for `postgres`

### CORS issues

Set:

```env
CORS_ALLOWED_ORIGINS=http://localhost:5173,http://localhost
```

Then restart affected backend services.

### Grafana login issues

Check `GRAFANA_PASSWORD` in `.env`, then recreate the Grafana container if needed.

## Kubernetes

Kubernetes manifests remain under `k8s/`, but they are not the primary verified release path documented in this guide. If you need a Kubernetes release, treat it as a separate validation effort from the Compose release.
