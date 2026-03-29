# Getting Started

This guide covers the verified ways to run WaterAccessOptimizer for this release.

## Prerequisites

For frontend-only work:

- Node.js 18+
- npm 9+

For the full stack:

- Docker Desktop 4.20+
- Docker Compose v2

For backend local development:

- Java 17+
- Maven 3.9+
- PostgreSQL 15+

## Fastest Path: Demo Mode

Use this when you want screenshots, UI review, or a no-login walkthrough.

```bash
cd frontend
npm install
npm run dev:demo
```

Open [http://localhost:5173](http://localhost:5173).

Demo mode does all of the following:

- enables the mock API layer
- seeds a logged-in demo session
- preloads dashboard, upload, map, and assessment data

## Full Stack With Docker

### 1. Configure environment

```bash
cp .env.example .env
```

Set at least these values:

```env
POSTGRES_PASSWORD=change-me
JWT_SECRET=change-me-with-at-least-32-characters
GRAFANA_PASSWORD=change-me-too
```

### 2. Start the release stack

```bash
docker-compose -f docker-compose.prod.yml up -d
```

### 3. Verify services

```bash
docker-compose -f docker-compose.prod.yml ps
docker-compose -f docker-compose.prod.yml logs --tail=100
```

### 4. Open the app

- Frontend: [http://localhost](http://localhost)
- Auth service: [http://localhost:8081](http://localhost:8081)
- Data service: [http://localhost:8087](http://localhost:8087)
- AI model: [http://localhost:8000](http://localhost:8000)
- Prometheus: [http://localhost:9090](http://localhost:9090)
- Grafana: [http://localhost:3000](http://localhost:3000)

## Frontend Local Development

```bash
cd frontend
npm install
npm run dev
```

Useful commands:

```bash
npm run lint
npm run test -- --run
npm run build
```

## Backend Local Development

The frontend expects:

- auth service on `http://localhost:8081/api/v1/auth`
- data service on `http://localhost:8087/api/v1`

Start the services separately:

```bash
cd backend/auth-service
mvn spring-boot:run
```

```bash
cd backend/api-gateway
mvn spring-boot:run
```

Note:

- the current release compose stack packages the data service from `backend/api-gateway/`
- the repository also contains `backend/data-service/`, but that is not the primary runtime path documented for this release

## Suggested Smoke Test

### Demo mode

1. Open `/dashboard`
2. Confirm recent uploads are listed
3. Open `/upload`
4. Open `/map`
5. Open `/assessment`
6. Open an assessment result page

### Docker stack

1. Open the frontend
2. Register or sign in
3. Upload a CSV file
4. Confirm upload history updates
5. Open the map page
6. Create and run an assessment

## Stopping Services

### Demo mode

Stop the Vite process with `Ctrl+C`.

### Docker

```bash
docker-compose -f docker-compose.prod.yml down
```

To remove volumes too:

```bash
docker-compose -f docker-compose.prod.yml down -v
```

## Troubleshooting

### Port conflict

Check these common ports:

- `80`
- `3000`
- `5432`
- `8000`
- `8081`
- `8087`
- `9090`

### Blank frontend

Run:

```bash
cd frontend
npm run build
```

If the build passes, the issue is usually environment or service connectivity rather than a broken bundle.

### Backend connectivity issues

Check:

```bash
curl http://localhost:8081/actuator/health
curl http://localhost:8087/actuator/health
```

## Current Verification

During release prep, the frontend was verified with:

- `npm run lint`
- `npm run test -- --run`
- `npm run build`
