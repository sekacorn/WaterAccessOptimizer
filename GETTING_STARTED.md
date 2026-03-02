# Getting Started with Water Access Optimizer

**Complete setup guide that actually works** - From zero to running application in 10 minutes.

---

## Prerequisites

Before you begin, ensure you have the following installed:

- **Docker Desktop** 4.20+ ([download](https://www.docker.com/products/docker-desktop/))
- **Git** 2.30+ ([download](https://git-scm.com/downloads))
- **8GB RAM minimum** (16GB recommended)
- **20GB free disk space**

### Verify Installation

```bash
# Check Docker
docker --version
# Expected: Docker version 24.0.0 or higher

# Check Docker Compose
docker compose version
# Expected: Docker Compose version v2.20.0 or higher

# Check Git
git --version
# Expected: git version 2.30.0 or higher
```

---

## Quick Start (5 Minutes)

### Step 1: Clone Repository

```bash
# Clone the repository
git clone https://github.com/your-org/WaterAccessOptimizer.git

# Navigate into the project
cd WaterAccessOptimizer
```

### Step 2: Configure Environment

```bash
# Copy environment template
cp .env.example .env

# (Optional) Edit with your API keys
# nano .env  # or use your favorite editor
```

**Minimal `.env` configuration** (works out of box):
```env
DB_PASSWORD=waterpass123
JWT_SECRET=dev_jwt_secret_minimum_32_characters_long_please
```

### Step 3: Start Services

```bash
# Start all services
docker-compose up -d

# This will:
# - Pull necessary Docker images (~2GB)
# - Build application containers (~5 minutes first time)
# - Start all services
# - Initialize database with schema
```

### Step 4: Wait for Services to be Healthy

```bash
# Check service status
docker-compose ps

# Wait for all services to show "healthy" status
# Typical wait time: 2-3 minutes

# Watch logs (optional)
docker-compose logs -f
```

### Step 5: Access the Application

Open your browser and navigate to:
- **Application**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **API Docs**: http://localhost:8080/swagger-ui.html

**Default Login**:
- Email: `admin@wateroptimizer.org`
- Password: `admin123`

**⚠️ Change the password immediately after first login!**

---

## Troubleshooting Quick Start

### Issue: "Port already in use"

```bash
# Check what's using the port
lsof -i :5432  # PostgreSQL
lsof -i :6379  # Redis
lsof -i :8080  # API Gateway
lsof -i :3000  # Frontend

# Kill the process or change ports in docker-compose.yml
```

### Issue: "Services won't start"

```bash
# Clean restart
docker-compose down -v  # WARNING: Removes all data
docker-compose up -d --build

# Check logs for errors
docker-compose logs postgres
docker-compose logs water-integrator
```

### Issue: "Cannot connect to database"

```bash
# Check PostgreSQL is running
docker-compose ps postgres

# Test database connection
docker-compose exec postgres psql -U wateradmin -d wateraccess -c "SELECT 1;"

# Expected output: " ?column? \n----------\n        1"
```

### Issue: "Frontend shows blank page"

```bash
# Check frontend logs
docker-compose logs frontend

# Restart frontend
docker-compose restart frontend

# Access directly
curl http://localhost:3000
```

---

## Development Setup (Hot Reload)

For active development with automatic code reloading:

```bash
# Start dev profile with hot-reload
docker-compose -f docker-compose.yml -f docker-compose.dev.yml up -d

# Now code changes will auto-reload:
# - Backend: Spring DevTools (Java)
# - Frontend: Vite HMR (React)

# View logs to see reload happening
docker-compose logs -f water-integrator
```

---

## Testing the Application

### Test 1: Upload Sample Data

1. Navigate to **Data > Upload**
2. Select "Community Data"
3. Upload the sample file: `tests/fixtures/data/community-data-valid.csv`
4. Click "Process Upload"
5. **Expected**: "2 records imported successfully"

### Test 2: View Communities on Map

1. Navigate to **Map**
2. **Expected**: See 2 communities plotted (Kalondama, Cacuso)
3. Click on a marker
4. **Expected**: See community details popup

### Test 3: Run Risk Assessment

1. Navigate to **Communities**
2. Select both communities (checkboxes)
3. Click "Run Assessment"
4. Wait ~10 seconds
5. **Expected**: See risk scores and recommendations

### Test 4: Check API

```bash
# Get JWT token
TOKEN=$(curl -X POST http://localhost:8080/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"admin@wateroptimizer.org","password":"admin123"}' \
  | jq -r '.access_token')

# List communities
curl http://localhost:8080/v1/data/communities \
  -H "Authorization: Bearer $TOKEN"

# Expected: JSON array with community data
```

---

## Monitoring (Optional)

Start monitoring stack to view metrics:

```bash
# Start with monitoring profile
docker-compose --profile monitoring up -d

# Access dashboards
open http://localhost:9090  # Prometheus
open http://localhost:3001  # Grafana (admin/admin)
```

**Grafana Dashboards**:
1. Login with admin/admin
2. Navigate to Dashboards → Browse
3. Open "Water Optimizer - System Overview"
4. See request rates, latency, errors, resource usage

---

## Stopping Services

```bash
# Stop all services (data preserved)
docker-compose down

# Stop and remove ALL data (clean slate)
docker-compose down -v

# Stop specific service
docker-compose stop water-integrator
```

---

## Next Steps

### 1. Explore the Application
- **Upload your own data** (CSV or GeoJSON)
- **Create custom risk assessments**
- **Export maps and reports**
- **Invite team members**

### 2. Read Documentation
- [User Guide](docs/USER_GUIDE.md)
- [API Documentation](http://localhost:8080/swagger-ui.html)
- [Admin Guide](docs/ADMIN_GUIDE.md)

### 3. Customize
- Edit `.env` for your API keys (USGS, LLM)
- Configure email settings for notifications
- Adjust risk assessment parameters

### 4. Contribute
- Read [CONTRIBUTING.md](CONTRIBUTING.md)
- Check [open issues](https://github.com/your-org/WaterAccessOptimizer/issues)
- Submit pull requests

---

## Common Workflows

### Daily Development

```bash
# Morning: Start services
docker-compose up -d

# Work on code (auto-reload enabled)
# ...

# Evening: Stop services
docker-compose down
```

### Testing Changes

```bash
# Run tests
docker-compose exec water-integrator mvn test

# Run integration tests
docker-compose -f docker-compose.test.yml up --abort-on-container-exit

# Check code coverage
open backend/water-integrator/target/site/jacoco/index.html
```

### Database Operations

```bash
# Backup database
docker-compose exec postgres pg_dump -U wateradmin wateraccess > backup.sql

# Restore database
docker-compose exec -T postgres psql -U wateradmin wateraccess < backup.sql

# Access database shell
docker-compose exec postgres psql -U wateradmin -d wateraccess
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f water-integrator

# Last 100 lines
docker-compose logs --tail=100 water-integrator

# Since 10 minutes ago
docker-compose logs --since=10m
```

---

## Performance Tips

### Speed up First Start

```bash
# Pre-pull images
docker-compose pull

# Build in parallel
docker-compose build --parallel
```

### Reduce Resource Usage

```bash
# Limit service replicas (in docker-compose.yml)
# For memory-constrained machines, run only essential services:

docker-compose up -d postgres redis water-integrator frontend
```

### Persistent Data

By default, database and cache data persist between restarts.

```bash
# List volumes
docker volume ls | grep wateraccessoptimizer

# Remove volumes (DANGER: data loss)
docker-compose down -v
```

---

## Support

Having trouble? Here's where to get help:

1. **Check Logs**: `docker-compose logs -f`
2. **Search Issues**: [GitHub Issues](https://github.com/your-org/WaterAccessOptimizer/issues)
3. **Ask Question**: [GitHub Discussions](https://github.com/your-org/WaterAccessOptimizer/discussions)
4. **Email Support**: support@wateroptimizer.org

---

## Success Checklist

After completing this guide, you should be able to:

- [x] Start all services with `docker-compose up -d`
- [x] Access the application at http://localhost:3000
- [x] Login with admin credentials
- [x] Upload sample community data
- [x] View communities on the map
- [x] Run a risk assessment
- [x] View Grafana dashboards (optional)
- [x] Stop services with `docker-compose down`

**Congratulations! You're ready to use Water Access Optimizer** 

---

**Last Updated**: January 2024
**Version**: 1.0.0
