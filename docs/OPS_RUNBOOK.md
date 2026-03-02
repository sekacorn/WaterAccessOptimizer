# Operations Runbook - Water Access Optimizer

## Quick Reference

### Service URLs
- **Application**: http://localhost:3000
- **API Gateway**: http://localhost:8080
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3001 (admin/admin)
- **Health Checks**: http://localhost:{PORT}/actuator/health

### Service Ports
| Service | Port |
|---------|------|
| Frontend | 3000 |
| API Gateway | 8080 |
| Water Integrator | 8081 |
| Water Visualizer | 8082 |
| User Session | 8083 |
| LLM Service | 8084 |
| Collaboration | 8085 |
| Auth Service | 8086 |
| PostgreSQL | 5432 |
| Redis | 6379 |
| Prometheus | 9090 |
| Grafana | 3001 |

---

## Starting the System

### Quick Start
```bash
# Start all services
docker-compose up -d

# Check service status
docker-compose ps

# View logs
docker-compose logs -f

# Stop all services
docker-compose down
```

### Health Check
```bash
# Check all services are healthy
curl http://localhost:8080/actuator/health
curl http://localhost:8081/actuator/health
curl http://localhost:8086/actuator/health

# Check Prometheus targets
open http://localhost:9090/targets

# Check Grafana dashboard
open http://localhost:3001
```

---

## Incident Response Procedures

### 1. Service Down Alert

**Symptoms**:
- Prometheus alert: `ServiceDown`
- Service returning 503 errors
- Health check endpoint unreachable

**Investigation**:
```bash
# Check service logs
docker-compose logs {service-name}

# Check service status
docker-compose ps {service-name}

# Check resource usage
docker stats {service-name}
```

**Resolution**:
```bash
# Restart specific service
docker-compose restart {service-name}

# If restart fails, rebuild and restart
docker-compose up -d --build {service-name}

# Check logs for startup errors
docker-compose logs -f {service-name}
```

---

### 2. High Error Rate (>5%)

**Symptoms**:
- Prometheus alert: `HighErrorRate`
- Many 5xx responses in logs
- Error rate dashboard panel showing spike

**Investigation**:
1. Check Grafana dashboard for affected service
2. Review logs for error patterns:
   ```bash
   docker-compose logs {service-name} | grep ERROR
   ```
3. Check recent deployments:
   ```bash
   git log -5 --oneline
   ```
4. Check external dependencies (database, Redis)

**Resolution**:
- If caused by recent deployment: rollback
- If database issue: check connection pool, slow queries
- If external API issue: implement circuit breaker
- If code bug: hotfix and deploy

---

### 3. Slow API Response (P95 > 2s)

**Symptoms**:
- Prometheus alert: `SlowAPIResponse`
- Users reporting slow page loads
- Latency dashboard showing increased P95

**Investigation**:
```bash
# Check database query performance
docker-compose exec postgres psql -U wateradmin -d wateraccess -c "
  SELECT query, mean_exec_time, calls
  FROM pg_stat_statements
  ORDER BY mean_exec_time DESC
  LIMIT 10;
"

# Check JVM metrics in Grafana
# - Heap usage
# - GC pauses
# - Thread count

# Check Redis performance
docker-compose exec redis redis-cli INFO stats
```

**Resolution**:
- Add database indexes for slow queries
- Enable query result caching
- Increase heap size if memory pressure
- Scale horizontally (add more service instances)

---

### 4. Database Connection Pool Exhausted

**Symptoms**:
- Prometheus alert: `DatabaseConnectionPoolExhausted`
- Services throwing connection timeout errors
- Logs showing "HikariPool connection timeout"

**Investigation**:
```bash
# Check active connections
docker-compose exec postgres psql -U wateradmin -d wateraccess -c "
  SELECT datname, count(*)
  FROM pg_stat_activity
  GROUP BY datname;
"

# Check for long-running queries
docker-compose exec postgres psql -U wateradmin -d wateraccess -c "
  SELECT pid, now() - pg_stat_activity.query_start AS duration, query
  FROM pg_stat_activity
  WHERE state = 'active'
  ORDER BY duration DESC;
"
```

**Resolution**:
```bash
# Increase pool size in application.yml
# spring.datasource.hikari.maximum-pool-size: 20

# Kill long-running queries (if stuck)
docker-compose exec postgres psql -U wateradmin -d wateraccess -c "
  SELECT pg_terminate_backend(pid)
  FROM pg_stat_activity
  WHERE state = 'active' AND now() - query_start > interval '10 minutes';
"

# Restart service to reset connections
docker-compose restart {service-name}
```

---

### 5. High Memory Usage (>85%)

**Symptoms**:
- Prometheus alert: `HighJVMMemoryUsage`
- Services becoming unresponsive
- OutOfMemoryError in logs

**Investigation**:
```bash
# Check heap dump
docker-compose exec {service-name} jmap -heap 1

# Check container memory stats
docker stats {service-name}

# Review recent code changes for memory leaks
```

**Resolution**:
```bash
# Increase heap size in Dockerfile
# ENV JAVA_OPTS="-Xmx2g -Xms1g"

# Rebuild and restart
docker-compose up -d --build {service-name}

# Monitor for recurrence
```

---

### 6. High Login Failure Rate

**Symptoms**:
- Prometheus alert: `HighLoginFailureRate`
- Many failed login attempts
- Possible brute force attack

**Investigation**:
```bash
# Check auth service logs
docker-compose logs auth-service | grep "login.failure"

# Check for repeated IPs
docker-compose logs auth-service | grep "login.failure" | awk '{print $NF}' | sort | uniq -c | sort -rn
```

**Resolution**:
- Implement rate limiting on login endpoint
- Enable MFA for affected accounts
- Block suspicious IPs in NGINX
- Alert security team if sustained attack

---

### 7. Database Unreachable

**Symptoms**:
- Prometheus alert: `DatabaseUnreachable`
- All services failing health checks
- Connection refused errors in logs

**Investigation**:
```bash
# Check postgres container status
docker-compose ps postgres

# Check postgres logs
docker-compose logs postgres

# Test connection manually
docker-compose exec postgres pg_isready -U wateradmin
```

**Resolution**:
```bash
# Restart postgres
docker-compose restart postgres

# If data corruption, restore from backup
docker-compose down
docker volume rm wateraccessoptimizer_postgres-data
# Restore backup here
docker-compose up -d
```

---

### 8. Redis Unreachable

**Symptoms**:
- Prometheus alert: `RedisUnreachable`
- Session management failures
- Cache misses increasing

**Investigation**:
```bash
# Check redis container
docker-compose ps redis

# Check redis logs
docker-compose logs redis

# Test connection
docker-compose exec redis redis-cli PING
```

**Resolution**:
```bash
# Restart redis
docker-compose restart redis

# Sessions will be lost but services will recover
# Monitor for session recreation
```

---

## Monitoring Dashboards

### Grafana Dashboard Panels

1. **Service Status** - Green/red indicators for each service
2. **Request Rate** - Requests per second by service
3. **Error Rate** - 5xx error percentage by service
4. **API Response Time** - P50, P95, P99 latency
5. **Database Connection Pool** - Active, idle, max connections
6. **JVM Heap Memory** - Used vs max heap by service
7. **Auth Operations** - Login success/failure, registrations
8. **Auth Duration** - Login and registration timing

---

## Metrics Reference

### Key Metrics to Watch

| Metric | Threshold | Severity |
|--------|-----------|----------|
| Service Up | < 1 | Critical |
| Error Rate | > 5% | Critical |
| P95 Latency | > 2s | Warning |
| DB Pool Usage | > 90% | Warning |
| Heap Usage | > 85% | Warning |
| Login Failure Rate | > 20% | Warning |

---

## Backup and Recovery

### Database Backup
```bash
# Manual backup
docker-compose exec postgres pg_dump -U wateradmin wateraccess > backup_$(date +%Y%m%d).sql

# Restore from backup
docker-compose exec -T postgres psql -U wateradmin wateraccess < backup_20240120.sql
```

### Log Retention
- Logs are kept for 7 days (rotating)
- JSON logs: `/var/log/water-optimizer/*.json`
- Archive older logs before deletion

---

## Scaling Procedures

### Horizontal Scaling (Multiple Instances)
```yaml
# In docker-compose.yml, add:
water-integrator:
  deploy:
    replicas: 3
```

### Vertical Scaling (More Resources)
```yaml
# Add resource limits
water-integrator:
  deploy:
    resources:
      limits:
        cpus: '2'
        memory: 2G
```

---

## Emergency Contacts

| Role | Contact | Escalation |
|------|---------|------------|
| On-Call Engineer | [Your Number] | Immediate |
| Database Admin | [DBA Number] | 30 min |
| DevOps Lead | [Lead Number] | 1 hour |
| CTO | [CTO Number] | Critical only |

---

## Change Log

| Date | Change | Author |
|------|--------|--------|
| 2024-01-26 | Initial runbook created | SRE Team |
