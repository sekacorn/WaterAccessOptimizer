# Deployment Guide - Water Access Optimizer

Complete deployment guide for local development, Docker, and Kubernetes production deployments.

---

## Table of Contents

1. [Prerequisites](#prerequisites)
2. [Local Development](#local-development)
3. [Docker Deployment](#docker-deployment)
4. [Kubernetes Deployment](#kubernetes-deployment)
5. [CI/CD Pipeline](#cicd-pipeline)
6. [Monitoring & Observability](#monitoring--observability)
7. [Troubleshooting](#troubleshooting)

---

## Prerequisites

### Required Software

**For Local Development:**
- Node.js 18+ (Frontend)
- Java 17+ (Backend)
- Maven 3.9+ (Backend build)
- PostgreSQL 15+ (Database)

**For Docker Deployment:**
- Docker 20.10+
- Docker Compose 2.0+

**For Kubernetes Deployment:**
- kubectl 1.25+
- Kubernetes cluster 1.25+ (minikube, kind, or cloud provider)
- Helm 3.0+ (optional, for package management)

---

## Local Development

### 1. Clone Repository

```bash
git clone https://github.com/your-org/WaterAccessOptimizer.git
cd WaterAccessOptimizer
```

### 2. Setup Database

```bash
# Install PostgreSQL 15
# Create database
createdb wateroptimizer

# Create user
psql wateroptimizer
CREATE USER wateradmin WITH PASSWORD 'your_password';
GRANT ALL PRIVILEGES ON DATABASE wateroptimizer TO wateradmin;
```

### 3. Configure Environment

```bash
# Copy environment template
cp .env.example .env

# Edit .env with your values
nano .env
```

### 4. Start Backend Services

**Auth Service (Port 8081):**
```bash
cd backend/auth-service
mvn clean install
mvn spring-boot:run
```

**Data Service (Port 8087):**
```bash
cd backend/api-gateway
mvn clean install
mvn spring-boot:run
```

### 5. Start Frontend

```bash
cd frontend
npm install
npm run dev
```

**Application URLs:**
- Frontend: http://localhost:5173
- Auth Service: http://localhost:8081
- Data Service: http://localhost:8087

---

## Docker Deployment

### Quick Start

```bash
# Copy environment file
cp .env.example .env

# Edit .env with production values
nano .env

# Start all services
docker-compose -f docker-compose.prod.yml up -d

# View logs
docker-compose -f docker-compose.prod.yml logs -f

# Stop all services
docker-compose -f docker-compose.prod.yml down
```

### Build Custom Images

```bash
# Build frontend
docker build -t water-optimizer/frontend:latest ./frontend

# Build auth service
docker build -t water-optimizer/auth-service:latest ./backend/auth-service

# Build data service
docker build -t water-optimizer/data-service:latest ./backend/api-gateway
```

### Access Application

**URLs:**
- Frontend: http://localhost
- Auth API: http://localhost:8081
- Data API: http://localhost:8087
- Prometheus: http://localhost:9090
- Grafana: http://localhost:3000

**Default Credentials:**
- Grafana: admin / (from .env GRAFANA_PASSWORD)

### Docker Compose Commands

```bash
# View running containers
docker-compose -f docker-compose.prod.yml ps

# View logs for specific service
docker-compose -f docker-compose.prod.yml logs -f frontend

# Restart service
docker-compose -f docker-compose.prod.yml restart auth-service

# Scale service
docker-compose -f docker-compose.prod.yml up -d --scale auth-service=3

# Remove all containers and volumes
docker-compose -f docker-compose.prod.yml down -v
```

---

## Kubernetes Deployment

### 1. Setup Kubernetes Cluster

**For local testing (minikube):**
```bash
minikube start --cpus=4 --memory=8192
minikube addons enable ingress
minikube addons enable metrics-server
```

**For production:** Use managed Kubernetes service (EKS, GKE, AKS)

### 2. Configure Secrets

```bash
# Generate base64 encoded secrets
echo -n 'your_secure_password' | base64
echo -n 'your_jwt_secret_key' | base64

# Edit k8s/secret.yaml with your values
nano k8s/secret.yaml
```

### 3. Deploy to Kubernetes

```bash
# Create namespace
kubectl apply -f k8s/namespace.yaml

# Apply configuration
kubectl apply -f k8s/configmap.yaml
kubectl apply -f k8s/secret.yaml

# Create persistent volume
kubectl apply -f k8s/postgres-pvc.yaml

# Deploy database
kubectl apply -f k8s/postgres-deployment.yaml

# Deploy backend services
kubectl apply -f k8s/auth-service-deployment.yaml
kubectl apply -f k8s/data-service-deployment.yaml

# Deploy frontend
kubectl apply -f k8s/frontend-deployment.yaml

# Setup ingress
kubectl apply -f k8s/ingress.yaml

# Setup auto-scaling (optional)
kubectl apply -f k8s/hpa.yaml
```

### 4. Verify Deployment

```bash
# Check pods
kubectl get pods -n water-optimizer

# Check services
kubectl get svc -n water-optimizer

# Check deployments
kubectl get deployments -n water-optimizer

# View logs
kubectl logs -f deployment/auth-service -n water-optimizer

# Get ingress address
kubectl get ingress -n water-optimizer
```

### 5. Access Application

**Via Ingress:**
- Configure DNS to point to ingress IP
- Access: https://water-optimizer.example.com

**Via Port Forward (for testing):**
```bash
# Frontend
kubectl port-forward svc/frontend 8080:80 -n water-optimizer

# Auth Service
kubectl port-forward svc/auth-service 8081:8081 -n water-optimizer

# Data Service
kubectl port-forward svc/data-service 8087:8087 -n water-optimizer
```

### Kubernetes Management Commands

```bash
# Scale deployment
kubectl scale deployment/auth-service --replicas=5 -n water-optimizer

# Update image
kubectl set image deployment/frontend frontend=water-optimizer/frontend:v2.0 -n water-optimizer

# Rollback deployment
kubectl rollout undo deployment/auth-service -n water-optimizer

# View rollout history
kubectl rollout history deployment/auth-service -n water-optimizer

# Restart deployment
kubectl rollout restart deployment/data-service -n water-optimizer

# Delete all resources
kubectl delete namespace water-optimizer
```

---

## CI/CD Pipeline

### GitHub Actions Setup

The CI/CD pipeline automatically:
1. Builds and tests code
2. Runs security scans
3. Builds Docker images
4. Pushes to container registry
5. Deploys to Kubernetes

### Required Secrets

Configure in GitHub Settings → Secrets:

```bash
# Docker/GitHub Container Registry
GITHUB_TOKEN  # Automatically provided

# Kubernetes Configuration
KUBE_CONFIG   # Base64 encoded kubeconfig file

# Application Secrets (optional, if not in cluster)
POSTGRES_PASSWORD
JWT_SECRET
GRAFANA_PASSWORD
```

### Triggering Deployments

**Automatic:**
- Push to `master` or `main` branch triggers full deployment
- Pull requests trigger build and test only

**Manual:**
```bash
# Via GitHub UI
Actions → CI/CD Pipeline → Run workflow

# Via API
curl -X POST \
  -H "Accept: application/vnd.github+json" \
  -H "Authorization: token YOUR_TOKEN" \
  https://api.github.com/repos/OWNER/REPO/actions/workflows/ci-cd.yml/dispatches \
  -d '{"ref":"main"}'
```

### Pipeline Stages

1. **Frontend Build & Test** (5-10 min)
   - Install dependencies
   - Run linter
   - Run unit tests
   - Build production bundle

2. **Backend Build & Test** (10-15 min)
   - Build with Maven
   - Run unit tests
   - Generate test reports

3. **E2E Tests** (5-10 min)
   - Run Playwright tests
   - Generate test reports

4. **Docker Build** (10-15 min)
   - Build images for all components
   - Push to container registry

5. **Deploy** (5-10 min)
   - Update Kubernetes deployments
   - Verify rollout status
   - Run smoke tests

**Total Pipeline Time:** ~35-60 minutes

---

## Monitoring & Observability

### Prometheus Metrics

**Access:** http://localhost:9090 (Docker) or via port-forward (K8s)

**Available Metrics:**
- JVM metrics (heap, threads, GC)
- HTTP request metrics
- Database connection pool
- Custom application metrics

**Example Queries:**
```promql
# Request rate
rate(http_server_requests_seconds_count[5m])

# Error rate
rate(http_server_requests_seconds_count{status=~"5.."}[5m])

# 95th percentile latency
histogram_quantile(0.95, rate(http_server_requests_seconds_bucket[5m]))
```

### Grafana Dashboards

**Access:** http://localhost:3000 (Docker) or via port-forward (K8s)

**Default Username:** admin
**Password:** From .env or secrets

**Pre-configured Dashboards:**
- JVM Dashboard
- Spring Boot Statistics
- Database Metrics
- Kubernetes Cluster Monitoring

### Logging

**Docker:**
```bash
# View logs
docker-compose logs -f [service-name]

# Follow specific service
docker-compose logs -f auth-service
```

**Kubernetes:**
```bash
# View pod logs
kubectl logs -f pod/auth-service-xxx -n water-optimizer

# View logs from all pods in deployment
kubectl logs -f deployment/auth-service -n water-optimizer

# View previous container logs
kubectl logs pod/auth-service-xxx --previous -n water-optimizer
```

### Health Checks

**Application Health:**
```bash
# Auth Service
curl http://localhost:8081/actuator/health

# Data Service
curl http://localhost:8087/actuator/health

# Frontend
curl http://localhost/health
```

**Database Health:**
```bash
# PostgreSQL
docker exec -it water-optimizer-db pg_isready -U wateradmin
```

---

## Troubleshooting

### Common Issues

#### 1. Database Connection Failed

**Symptoms:** Backend services fail to start with connection errors

**Solutions:**
```bash
# Check PostgreSQL is running
docker-compose ps postgres
kubectl get pods -l app=postgres -n water-optimizer

# Verify credentials
# Check .env file or K8s secrets

# Check database logs
docker-compose logs postgres
kubectl logs -l app=postgres -n water-optimizer

# Test connection manually
psql -h localhost -U wateradmin -d wateroptimizer
```

#### 2. Frontend Can't Reach Backend

**Symptoms:** API calls fail with 404 or CORS errors

**Solutions:**
```bash
# Check backend services are running
curl http://localhost:8081/actuator/health
curl http://localhost:8087/actuator/health

# Verify environment variables
# Check VITE_API_BASE_URL in .env

# Check CORS configuration
# Review backend application.yml ALLOWED_ORIGINS
```

#### 3. Out of Memory (OOM)

**Symptoms:** Pods/containers crash with OOM errors

**Solutions:**
```bash
# Increase memory limits in deployment YAML
# For Docker: modify docker-compose.yml memory limits
# For K8s: modify deployment resources.limits.memory

# Check current memory usage
docker stats
kubectl top pods -n water-optimizer
```

#### 4. Image Pull Errors

**Symptoms:** Kubernetes can't pull Docker images

**Solutions:**
```bash
# Verify image exists
docker images | grep water-optimizer

# Check registry credentials
kubectl get secrets -n water-optimizer

# Create image pull secret
kubectl create secret docker-registry regcred \
  --docker-server=ghcr.io \
  --docker-username=USERNAME \
  --docker-password=TOKEN \
  -n water-optimizer
```

#### 5. Ingress Not Working

**Symptoms:** Can't access application via domain

**Solutions:**
```bash
# Check ingress controller is installed
kubectl get pods -n ingress-nginx

# Verify ingress resource
kubectl describe ingress water-optimizer-ingress -n water-optimizer

# Check DNS resolution
nslookup water-optimizer.example.com

# Test with port-forward instead
kubectl port-forward svc/frontend 8080:80 -n water-optimizer
```

### Diagnostic Commands

```bash
# Docker
docker-compose ps
docker-compose logs --tail=100 [service]
docker inspect [container-id]
docker stats

# Kubernetes
kubectl get all -n water-optimizer
kubectl describe pod [pod-name] -n water-optimizer
kubectl logs -f [pod-name] -n water-optimizer
kubectl top pods -n water-optimizer
kubectl get events -n water-optimizer --sort-by='.lastTimestamp'
```

### Performance Debugging

```bash
# Check application metrics
curl http://localhost:8081/actuator/metrics
curl http://localhost:8087/actuator/metrics

# View thread dumps
curl http://localhost:8081/actuator/threaddump

# View heap dump (warning: large file)
curl http://localhost:8081/actuator/heapdump > heapdump.hprof
```

---

## Backup & Restore

### Database Backup

**Docker:**
```bash
# Backup
docker exec water-optimizer-db pg_dump -U wateradmin wateroptimizer > backup.sql

# Restore
docker exec -i water-optimizer-db psql -U wateradmin wateroptimizer < backup.sql
```

**Kubernetes:**
```bash
# Backup
kubectl exec -i postgres-xxx -n water-optimizer -- pg_dump -U wateradmin wateroptimizer > backup.sql

# Restore
kubectl exec -i postgres-xxx -n water-optimizer -- psql -U wateradmin wateroptimizer < backup.sql
```

### Automated Backups

**Using CronJob (Kubernetes):**
```yaml
apiVersion: batch/v1
kind: CronJob
metadata:
  name: postgres-backup
  namespace: water-optimizer
spec:
  schedule: "0 2 * * *"  # Daily at 2 AM
  jobTemplate:
    spec:
      template:
        spec:
          containers:
          - name: backup
            image: postgres:15-alpine
            command:
            - /bin/sh
            - -c
            - pg_dump -h postgres -U wateradmin wateroptimizer > /backup/backup-$(date +%Y%m%d).sql
```

---

## Security Best Practices

1. **Secrets Management**
   - Never commit secrets to git
   - Use Kubernetes Secrets or external secret managers
   - Rotate secrets regularly

2. **Network Security**
   - Use TLS/HTTPS for all external traffic
   - Implement network policies in Kubernetes
   - Restrict database access to backend only

3. **Container Security**
   - Run containers as non-root user
   - Scan images for vulnerabilities
   - Keep base images updated

4. **Access Control**
   - Use RBAC in Kubernetes
   - Implement least privilege principle
   - Enable audit logging

---

## Scaling Recommendations

**Small Deployment (< 1000 users):**
- Frontend: 2 replicas
- Auth Service: 2 replicas
- Data Service: 2 replicas
- Database: 1 instance
- Total: ~4GB RAM, 2 CPU cores

**Medium Deployment (< 10,000 users):**
- Frontend: 3 replicas
- Auth Service: 3 replicas
- Data Service: 5 replicas
- Database: 1 instance with read replicas
- Total: ~12GB RAM, 6 CPU cores

**Large Deployment (> 10,000 users):**
- Frontend: 5+ replicas with CDN
- Auth Service: 5+ replicas
- Data Service: 10+ replicas
- Database: Clustered with read replicas
- Total: ~32GB+ RAM, 16+ CPU cores

---

## Support & Resources

- **Documentation:** `docs/` directory
- **Issues:** GitHub Issues
- **Discussions:** GitHub Discussions
- **Wiki:** Project Wiki

---

**Last Updated:** 2026-02-04
**Version:** 1.0.0
